package com.fc.interblog.ServiceImpl;

import com.fc.interblog.Constant.Constraints;
import com.fc.interblog.Constant.ExceptionMessage;
import com.fc.interblog.Constant.UserConstant;
import com.fc.interblog.Dao.*;
import com.fc.interblog.Entity.*;
import com.fc.interblog.Global.SessionManager;
import com.fc.interblog.Service.UserService;
import com.fc.interblog.Utils.ServerException;
import com.fc.interblog.Utils.Transport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.persistence.Tuple;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private UserDao userDao;
    private AuthDao authDao;
    private PostDao postDao;
    private InterestDao interestDao;
    private VisitDao visitDao;
    private BanRecordDao banRecordDao;
    private RemarkItemDao remarkItemDao;
    private SessionManager sessionManager;

    @Autowired
    public UserServiceImpl(UserDao userDao, AuthDao authDao, PostDao postDao, InterestDao interestDao, VisitDao visitDao, BanRecordDao banRecordDao, RemarkItemDao remarkItemDao, SessionManager sessionManager) {
        this.userDao = userDao;
        this.authDao = authDao;
        this.postDao = postDao;
        this.interestDao = interestDao;
        this.visitDao = visitDao;
        this.banRecordDao = banRecordDao;
        this.remarkItemDao = remarkItemDao;
        this.sessionManager = sessionManager;
    }

    private static String byte2Hex(byte[] bytes){
        StringBuilder stringBuffer = new StringBuilder();
        String temp;
        for (byte aByte : bytes) {
            temp = Integer.toHexString(aByte & 0xFF);
            if (temp.length() == 1)
                stringBuffer.append("0");
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    private static String hash(String password) {
        MessageDigest digest;
        String passwordHash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            passwordHash = byte2Hex(digest.digest());
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return passwordHash;
    }

    @Override
    public Transport.LoginResponse register(User user, String email, String password) throws ServerException {
        if (email.length() > Constraints.EMAIL_MAX
                || password.length() < Constraints.PASSWORD_MIN
                || password.length() > Constraints.PASSWORD_MAX
                || user.getNickname().length() > Constraints.NICKNAME_MAX
                || (user.getProfile() != null && user.getProfile().length() > Constraints.PROFILE_MAX)
                || (user.getIcon() != null && user.getIcon().length() > Constraints.ICON_MAX * 1.5))
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        if (authDao.existsEmail(email))
            throw new ServerException(ExceptionMessage.EMAIL_EXISTS);
        if (userDao.existsNickname(user.getNickname()))
            throw new ServerException(ExceptionMessage.NICKNAME_EXISTS);
        Auth auth = new Auth();
        auth.setBanned(false);
        auth.setEmail(email);
        auth.setPasswordHash(hash(password));
        auth = authDao.addAuth(auth);
        user.setUserId(auth.getUserId());
        user.setPostCount(0L);
        user.setFollowCount(0L);
        user.setFollowerCount(0L);
        return new Transport.LoginResponse(userDao.addUser(user), sessionManager);
    }

    @Override
    public Transport.BanResponse banUser(Long uid, String role, Boolean ban, Long targetId, String bannedMessage) throws ServerException {
        if (ban && bannedMessage.length() > Constraints.BAN_REASON_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        User target = userDao.getUserInfoById(targetId);
        String targetRole = target.getRole();
        if (role.equals(UserConstant.ADMINISTRATOR) && !targetRole.equals(UserConstant.ADMINISTRATOR)
                || role.equals(UserConstant.USER_MANAGER) && targetRole.equals(UserConstant.NORMAL_USER)) {
            Auth auth = authDao.getAuthById(targetId);
            if (ban && auth.getBanned())
                throw new ServerException(ExceptionMessage.USER_ALREADY_BANNED);
            if (!ban && !auth.getBanned())
                throw new ServerException(ExceptionMessage.USER_ALREADY_RECOVERED);
            if (ban) {
                auth.setBanned(true);
                authDao.saveAuth(auth);
                Date tm = new Date();
                BanRecord record = new BanRecord();
                record.setBannedBy(uid);
                record.setBannedId(targetId);
                record.setBannedTime(tm);
                record.setReason(bannedMessage);
                banRecordDao.addRecord(record);
                return new Transport.BanResponse(tm, uid, userDao.getUserInfoById(uid).getNickname());
            } else {
                auth.setBanned(false);
                authDao.saveAuth(auth);
                Date tm = new Date();
                banRecordDao.completeRecord(targetId, uid, tm);
                return new Transport.BanResponse(tm, uid, userDao.getUserInfoById(uid).getNickname());
            }
        }
        else {
            throw new ServerException(ExceptionMessage.FORBIDDEN);
        }
    }

    @Override
    public Transport.MyInfoResponse getMyInfo(Long id, Boolean noIcon) throws ServerException {
        return new Transport.MyInfoResponse(authDao.getAuthById(id), userDao.getUserById(id), postDao.getUserPostCount(id), noIcon);
    }

    @Override
    public Boolean getBanned(Long id) throws ServerException {
        return authDao.getAuthById(id).getBanned();
    }

    @Override
    public ArrayList<Transport.IdentityInfoResponse> getUserIdentityInfos(List<Long> ids) throws ServerException {
        ArrayList<Transport.IdentityInfoResponse> result = new ArrayList<>();
        for (Long id : ids)
            result.add(new Transport.IdentityInfoResponse(userDao.getUserById(id)));
        return result;
    }

    @Override
    public void setMyInfo(Long userId, Transport.MyInfoForm form) throws ServerException {
        User user = userDao.getUserById(userId);
        if (form.getNickname() != null && !form.getNickname().equals(user.getNickname()) && userDao.existsNickname(form.getNickname()))
            throw new ServerException(ExceptionMessage.NICKNAME_EXISTS);
        form.update(this, user);
        if (user.getNickname().length() > Constraints.NICKNAME_MAX
                || (user.getProfile() != null && user.getProfile().length() > Constraints.PROFILE_MAX)
                || (user.getIcon() != null && user.getIcon().length() > Constraints.ICON_MAX * 1.5)
                || user.getInterests().size() > Constraints.USER_INTEREST_LIMIT)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        if (form.getIcon() != null || form.getProfile() != null)
            userDao.saveUser(user);
        else
            userDao.saveUserInfo(user);
    }

    @Override
    public Transport.PublicInfoResponse getUserPublicInfo(Long me, Long userId) throws ServerException {
        return new Transport.PublicInfoResponse(userDao.getUserById(userId), postDao.getUserPostCount(userId), getFollowed(me, userId));
    }

    @Override
    public ArrayList<Transport.UserListItem> getUsers(Long startId, Long count, String orderBy, Boolean increase, String name) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<User> users = userDao.getUsers(startId, count, orderBy, increase, name);
        ArrayList<Transport.UserListItem> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.UserListItem(user, authDao, userDao));
        return result;
    }

    @Override
    public Transport.LoginResponse authorize(String email, String password) throws ServerException {
        Auth auth = authDao.authorize(email, hash(password));
        if (auth == null)
            throw new ServerException(ExceptionMessage.AUTHORIZATION_FAILED);
        if (auth.getBanned())
            throw new ServerException(ExceptionMessage.BANNED_FROM_LOGIN_FOR_REASON + getBannedReason(auth.getUserId()));
        return new Transport.LoginResponse(userDao.getUserById(auth.getUserId()), sessionManager);
    }

    @Override
    public boolean existsEmail(String email) {
        return authDao.existsEmail(email);
    }

    @Override
    public boolean existsNickname(String name) {
        return userDao.existsNickname(name);
    }

    @Override
    public String getBannedReason(Long userId) {
        BanRecord record = banRecordDao.getLatestRecord(userId);
        if (record != null)
            return record.getReason();
        else
            return "";
    }

    @Override
    public void setUserRole(Long id, String role) throws ServerException {
        User user = userDao.getUserInfoById(id);
        user.setRole(role);
        userDao.saveUserInfo(user);
    }

    @Override
    public ArrayList<Transport.BanRecordItem> getBanRecords(Long id) throws ServerException {
        List<BanRecord> list = banRecordDao.getBanRecords(id);
        ArrayList<Transport.BanRecordItem> result = new ArrayList<>();
        for (BanRecord item : list)
            result.add(new Transport.BanRecordItem(item, userDao));
        return result;
    }

    @Override
    public ArrayList<Transport.PostListItem> getPosts(Long me, Long id, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<Post> posts = postDao.getUserPosts(id, startId, count, me.equals(id));
        ArrayList<Transport.PostListItem> items = new ArrayList<>();
        for (Post post : posts)
            items.add(new Transport.PostListItem(post, me, userDao, postDao));
        visit(me, id);
        return items;
    }

    @Override
    public ArrayList<Transport.PostListItem> getAllPosts(Long me, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<Post> posts = postDao.getAllPosts(startId, count);
        ArrayList<Transport.PostListItem> items = new ArrayList<>();
        for (Post post : posts)
            items.add(new Transport.PostListItem(post, me, userDao, postDao));
        return items;
    }

    @Override
    public ArrayList<Transport.PostListItem> getDeletedPosts(Long me, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<Post> posts = postDao.getDeletedPosts(startId, count);
        ArrayList<Transport.PostListItem> items = new ArrayList<>();
        for (Post post : posts)
            items.add(new Transport.PostListItem(post, me, userDao, postDao));
        return items;
    }

    @Override
    public ArrayList<Transport.PostListItem> getHomePage(Long id, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<Post> posts = postDao.getHomePage(id, startId, count);
        ArrayList<Transport.PostListItem> result = new ArrayList<>();
        for (Post post : posts)
            result.add(new Transport.PostListItem(post, id, userDao, postDao));
        return result;
    }

    @Override
    public ArrayList<Transport.PostListItem> searchPostsByContent(Long me, String text, Boolean deleted, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<Post> posts = postDao.searchPostsByContent(text, deleted, startId, count);
        ArrayList<Transport.PostListItem> items = new ArrayList<>();
        for (Post post : posts)
            items.add(new Transport.PostListItem(post, me, userDao, postDao));
        return items;
    }

    @Override
    public void follow(Long u1, Long u2, Boolean follow) throws ServerException {
        boolean found = userDao.getFollowed(u1, u2);
        if (!found && follow) {
            User user1 = userDao.getUserInfoById(u1), user2 = userDao.getUserInfoById(u2);
            user1.setFollowCount(user1.getFollowCount() + 1);
            user2.setFollowerCount(user2.getFollowerCount() + 1);
            userDao.saveUserInfo(user1);
            userDao.saveUserInfo(user2);
            userDao.follow(u1, u2);
        }
        else if (found && !follow) {
            User user1 = userDao.getUserInfoById(u1), user2 = userDao.getUserInfoById(u2);
            user1.setFollowCount(user1.getFollowCount() - 1);
            user2.setFollowerCount(user2.getFollowerCount() - 1);
            userDao.saveUserInfo(user1);
            userDao.saveUserInfo(user2);
            userDao.unFollow(u1, u2);
        }
    }

    @Override
    public ArrayList<Transport.AbstractResponse> getFollows(Long id, Long start, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<User> users = userDao.getFollows(id, start, count);
        ArrayList<Transport.AbstractResponse> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.AbstractResponse(user));
        return result;
    }

    @Override
    public ArrayList<Transport.AbstractResponse> getFollowers(Long id, Long start, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<User> users = userDao.getFollowers(id, start, count);
        ArrayList<Transport.AbstractResponse> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.AbstractResponse(user));
        return result;
    }

    @Override
    public Boolean getFollowed(Long u1, Long u2) {
        return userDao.getFollowed(u1, u2);
    }

    @Override
    public void visit(Long u1, Long u2) {
        if (!u1.equals(u2)) {
            visitDao.removeVisit(u1, u2);
            Visit visit = new Visit();
            visit.setVisitor(u1);
            visit.setVisited(u2);
            visit.setTime(new Date());
            visitDao.addVisit(visit);
        }
    }

    @Override
    public ArrayList<Transport.VisitResponse> getVisitors(Long id) throws ServerException {
        List<Visit> visits = visitDao.getVisitors(id);
        ArrayList<Transport.VisitResponse> result = new ArrayList<>();
        for (Visit visit : visits)
            result.add(new Transport.VisitResponse(userDao.getUserById(visit.getVisitor()), visit));
        return result;
    }

    @Override
    public void matchInterests(User user, List<String> titles) throws ServerException {
        List<Interest> result = new ArrayList<>();
        for (String title : titles) {
            Interest interest = interestDao.findInterestByTitle(title);
            if (interest == null)
                throw new ServerException(ExceptionMessage.FOLLOWING_INTERESTS_NOT_EXIST + title);
            result.add(interest);
        }
        user.setInterests(result);
    }

    @Override
    public void likePost(Long userId, Long postId, Boolean like) throws ServerException {
        boolean liked = userDao.getLiked(userId, postId);
        if (like.equals(liked))
            return;
        Post post;
        try {
            post = postDao.getPostInfoById(postId);
        } catch (EntityNotFoundException e) {
            throw new ServerException(ExceptionMessage.POST_NOT_FOUND);
        }
        if (like) {
            post.setLikeCount(post.getLikeCount() + 1);
            postDao.savePostInfo(post);
            userDao.likePost(userId, postId);
        }
        else {
            post.setLikeCount(post.getLikeCount() - 1);
            postDao.savePostInfo(post);
            userDao.dislikePost(userId, postId);
        }
    }

    @Override
    public void likeRemark(Long userId, Long remarkId, Boolean like) throws ServerException {
        boolean liked = userDao.getLikedRemark(userId, remarkId);
        if (like.equals(liked))
            return;
        RemarkItem remark = remarkItemDao.getRemarkById(remarkId);
        if (like) {
            remark.setLikeCount(remark.getLikeCount() + 1);
            remarkItemDao.saveRemark(remark);
            userDao.likeRemark(userId, remarkId);
        }
        else {
            remark.setLikeCount(remark.getLikeCount() - 1);
            remarkItemDao.saveRemark(remark);
            userDao.dislikeRemark(userId, remarkId);
        }
    }

    @Override
    public List<String> searchInterest(String title) {
        return interestDao.searchInterest(title);
    }

    @Override
    public ArrayList<Transport.SearchUserResponse> searchUserByInterest(String title, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<User> users = userDao.searchUserByInterest(title, startId, count);
        ArrayList<Transport.SearchUserResponse> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.SearchUserResponse(user));
        return result;
    }

    @Override
    public ArrayList<Transport.SearchUserResponse> searchUser(String name, Long startId, Long count) throws ServerException {
        if (count > Constraints.COUNT_MAX)
            throw new ServerException(ExceptionMessage.INVALID_PARAMETER);
        List<User> users = userDao.searchUser(name, startId, count);
        ArrayList<Transport.SearchUserResponse> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.SearchUserResponse(user));
        return result;
    }

    @Override
    public List<Transport.PopularInterestItem> popularInterests() {
        List<Tuple> list = userDao.popularInterests();
        List<Transport.PopularInterestItem> result = new ArrayList<>();
        for (Tuple tuple: list)
            result.add(new Transport.PopularInterestItem(tuple.get(0).toString(), Long.parseLong(tuple.get(1).toString())));
        return result;
    }

    @Override
    public ArrayList<Transport.IdentityInfoResponse> getPopular() throws ServerException {
        List<User> users = userDao.getPopular();
        ArrayList<Transport.IdentityInfoResponse> result = new ArrayList<>();
        for (User user : users)
            result.add(new Transport.IdentityInfoResponse(user));
        return result;
    }

    @Override
    public void flush() {
        authDao.flush();
        userDao.flush();
        visitDao.flush();
        interestDao.flush();
    }
}
