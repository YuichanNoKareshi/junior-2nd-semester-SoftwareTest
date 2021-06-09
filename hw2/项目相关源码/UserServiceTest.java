package com.fc.interblog.Service;

import com.fc.interblog.Constant.Constraints;
import com.fc.interblog.Constant.ExceptionMessage;
import com.fc.interblog.Constant.UserConstant;
import com.fc.interblog.Dao.*;
import com.fc.interblog.Entity.*;
import com.fc.interblog.Global.SessionManager;
import com.fc.interblog.Utils.ServerException;
import com.fc.interblog.Utils.Transport;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityNotFoundException;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/*@Ignore*/
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserServiceTest {
    @Autowired
    UserService userService;
    @MockBean
    private UserDao userDao;
    @MockBean
    private AuthDao authDao;
    @MockBean
    private PostDao postDao;
    @MockBean
    private InterestDao interestDao;
    @MockBean
    private VisitDao visitDao;
    @MockBean
    private BanRecordDao banRecordDao;
    @MockBean
    private RemarkItemDao remarkItemDao;
    @MockBean
    private SessionManager sessionManager;

    private String sessionId;

    @Before
    public void mockSessionManager() {
        sessionId = "123234";
        doReturn(new Transport.SessionRecord(12L, "ad")).when(sessionManager).getInfo(sessionId);
        doNothing().when(sessionManager).endSession(anyString());
        doReturn(sessionId).when(sessionManager).getSessionId(12L, "ad");
    }

    @Test
    public void registerAndLogin() throws ServerException {
        String email = "123@qq.com";
        String password = "123123";
        String nickname = "nickname";
        String reason = "banned reason";

        User user = new User();
        user.setUserId(5L);
        user.setNickname(nickname);
        user.setRole("ts");
        Auth auth = new Auth();
        auth.setUserId(5L);
        auth.setBanned(false);
        doReturn(false).when(userDao).existsNickname(any());
        doReturn(false).when(authDao).existsEmail(any());
        doReturn(user).when(userDao).addUser(any());
        doReturn(auth).when(authDao).addAuth(any());
        doReturn(sessionId).when(sessionManager).getSessionId(5L, "ts");
        userService.register(user, email, password);
        String finalEmail = email;
        verify(authDao, times(1)).addAuth(argThat(item -> item.getEmail().equals(finalEmail)));
        verify(userDao, times(1)).addUser(argThat(item -> item.getNickname().equals(nickname)));

        doReturn(true).when(userDao).existsNickname(any());
        Exception e = assertThrows(ServerException.class, () -> userService.register(user, finalEmail, password));
        assertEquals(ExceptionMessage.NICKNAME_EXISTS, e.getMessage());
        doReturn(true).when(authDao).existsEmail(any());
        e = assertThrows(ServerException.class, () -> userService.register(user, finalEmail, password));
        assertEquals(ExceptionMessage.EMAIL_EXISTS, e.getMessage());

        doReturn(auth).when(authDao).authorize(any(), any());
        doReturn(user).when(userDao).getUserById(5L);
        doReturn(user).when(userDao).getUserInfoById(5L);
        assertEquals(nickname, userService.authorize(email, password).getNickname());

        BanRecord record = new BanRecord();
        record.setReason(reason);
        auth.setBanned(true);
        doReturn(record).when(banRecordDao).getLatestRecord(5L);
        e = assertThrows(ServerException.class, () -> userService.authorize(finalEmail, password));
        assertEquals(ExceptionMessage.BANNED_FROM_LOGIN_FOR_REASON + reason, e.getMessage());

        auth.setBanned(false);
        doReturn(null).when(authDao).authorize(any(), any());
        e = assertThrows(ServerException.class, () -> userService.authorize(finalEmail, password));
        assertEquals(ExceptionMessage.AUTHORIZATION_FAILED, e.getMessage());

        while (email.length() <= Constraints.EMAIL_MAX)
            email += email;
        String finalEmail1 = email;
        assertThrows(ServerException.class, ()->userService.register(user, finalEmail1, password));
    }

    @Test
    public void getInfo() throws ServerException {
        String title = "interest title";
        String icon = "icon content";

        Auth auth = new Auth();
        User user = new User();
        user.setFollowCount(10L);
        user.setFollowerCount(15L);
        user.setIcon(icon);
        List<Interest> interests = new ArrayList<>();
        Interest interest = new Interest();
        interest.setTitle(title);
        interests.add(interest);
        user.setInterests(interests);
        doReturn(auth).when(authDao).getAuthById(5L);
        doReturn(user).when(userDao).getUserInfoById(5L);
        doReturn(user).when(userDao).getUserById(5L);
        doReturn(100L).when(postDao).getUserPostCount(5L);
        Transport.MyInfoResponse response = userService.getMyInfo(5L, false);
        assertTrue(response.getIcon().equals(icon) && response.getInterests().get(0).equals(title));

        List<Long> ids = new ArrayList<>();
        ids.add(5L);
        assertEquals(icon, userService.getUserIdentityInfos(ids).get(0).getIcon());

        assertEquals(icon, userService.getUserPublicInfo(5L, 5L).getIcon());
    }

    @Test
    public void setUserData() throws ServerException {
        String title = "interest title";
        String nickname = "nickname";
        String newName = "new nickname";
        String profile = "my profile";

        Transport.MyInfoForm form = new Transport.MyInfoForm();
        form.setBirthday("2020-07-24");
        form.setGender('M');
        form.setIcon("icon");
        List<String> interestTitles = new ArrayList<>();
        interestTitles.add(title);
        form.setInterests(interestTitles);
        form.setNickname(newName);
        form.setPhone("111");
        form.setProfile(profile);
        User user = new User();
        user.setNickname(nickname);
        doReturn(user).when(userDao).getUserInfoById(5L);
        doReturn(user).when(userDao).getUserById(5L);
        doReturn(false).when(userDao).existsNickname(any());
        doReturn(new Interest()).when(interestDao).findInterestByTitle(title);
        doNothing().when(userDao).saveUser(any());
        doNothing().when(userDao).saveUserInfo(any());
        userService.setMyInfo(5L, form);

        verify(userDao).saveUser(argThat(item -> item.getNickname().equals(newName) && item.getProfile().equals(profile)));

        form.setIcon(null);
        form.setProfile(null);
        userService.setMyInfo(5L, form);
        verify(userDao).saveUserInfo(argThat(item -> item.getNickname().equals(newName)));

        doReturn(true).when(userDao).existsNickname(any());
        form.setNickname(nickname);
        Exception e = assertThrows(ServerException.class, () -> userService.setMyInfo(5L, form));
        assertEquals(ExceptionMessage.NICKNAME_EXISTS, e.getMessage());

        doReturn(false).when(userDao).existsNickname(any());
        doReturn(null).when(interestDao).findInterestByTitle(title);
        e = assertThrows(ServerException.class, () -> userService.setMyInfo(5L, form));
        assertEquals(ExceptionMessage.FOLLOWING_INTERESTS_NOT_EXIST + title, e.getMessage());

        doReturn(user).when(userDao).getUserInfoById(5L);
        doReturn(user).when(userDao).getUserInfoById(5L);
        doReturn(new Interest()).when(interestDao).findInterestByTitle(title);
        while (nickname.length() <= Constraints.NICKNAME_MAX)
            nickname += nickname;
        form.setNickname(nickname);
        e = assertThrows(ServerException.class, ()->userService.setMyInfo(5L, form));
        assertEquals(ExceptionMessage.INVALID_PARAMETER, e.getMessage());
    }

    @Test
    public void getUsers() throws ServerException {
        String email = "123@qq.com";
        String title = "interest title";
        String icon = "icon data";
        List<Interest> interests = new ArrayList<>();
        Interest interest = new Interest();
        interest.setTitle(title);
        interests.add(interest);
        List<User> list = new ArrayList<>();
        User user = new User();
        user.setUserId(5L);
        user.setIcon(icon);
        user.setInterests(interests);
        list.add(user);
        doReturn(list).when(userDao).getUsers(-1L, 5L, "id", true, "name");
        Auth auth = new Auth();
        auth.setEmail(email);
        auth.setBanned(true);
        doReturn(auth).when(authDao).getAuthById(5L);

        assertEquals(icon, userService.getUsers(-1L, 5L, "id", true, "name").get(0).getIcon());

        doThrow(new ServerException("")).when(authDao).getAuthById(any());
        assertThrows(ServerException.class, () -> userService.getUsers(-1L, 5L, "id", true, "name"));

        assertThrows(ServerException.class, () -> userService.getUsers(-1L, Constraints.COUNT_MAX + 1L, "id", true, "name"));
    }

    @Test
    public void checkIf() throws ServerException {
        String text = "test text";
        doReturn(false).when(userDao).existsNickname(text);
        assertFalse(userService.existsNickname(text));
        doReturn(true).when(userDao).existsNickname(text);
        assertTrue(userService.existsNickname(text));
        doReturn(false).when(authDao).existsEmail(text);
        assertFalse(userService.existsEmail(text));
        doReturn(true).when(authDao).existsEmail(text);
        assertTrue(userService.existsEmail(text));

        Auth auth = new Auth();
        auth.setBanned(false);
        doReturn(auth).when(authDao).getAuthById(5L);
        assertFalse(userService.getBanned(5L));
        auth.setBanned(true);
        assertTrue(userService.getBanned(5L));

        doReturn(false).when(userDao).getFollowed(5L, 7L);
        assertFalse(userService.getFollowed(5L, 7L));
        doReturn(true).when(userDao).getFollowed(5L, 7L);
        assertTrue(userService.getFollowed(5L, 7L));
    }

    @Test
    public void getBanReason() {
        String reason = "banned reason";
        BanRecord record = new BanRecord();
        record.setReason(reason);
        doReturn(record).when(banRecordDao).getLatestRecord(5L);
        assertEquals(reason, userService.getBannedReason(5L));
        doReturn(null).when(banRecordDao).getLatestRecord(5L);
        assertEquals("", userService.getBannedReason(5L));
    }

    @Test
    public void banUser() throws ServerException {
        String nickname = "admin";
        String message = "banned reason";
        User target = new User();
        target.setRole(UserConstant.NORMAL_USER);
        doReturn(target).when(userDao).getUserById(5L);
        doReturn(target).when(userDao).getUserInfoById(5L);
        Auth targetAuth = new Auth();
        targetAuth.setBanned(false);
        targetAuth.setUserId(5L);
        doReturn(targetAuth).when(authDao).getAuthById(5L);
        doNothing().when(authDao).saveAuth(any());
        doNothing().when(banRecordDao).addRecord(any());
        User admin = new User();
        admin.setNickname(nickname);
        doReturn(admin).when(userDao).getUserInfoById(7L);
        doReturn(admin).when(userDao).getUserById(7L);
        Exception e = assertThrows(ServerException.class, () -> userService.banUser(7L, UserConstant.ADMINISTRATOR, false, 5L, null));
        assertEquals(ExceptionMessage.USER_ALREADY_RECOVERED, e.getMessage());

        userService.banUser(7L, UserConstant.ADMINISTRATOR, true, 5L, message);
        verify(authDao).saveAuth(argThat(item -> item.getUserId() == 5L && item.getBanned()));
        String finalMessage = message;
        verify(banRecordDao).addRecord(argThat(item -> item.getReason().equals(finalMessage)));

        e = assertThrows(ServerException.class, () -> userService.banUser(7L, UserConstant.ADMINISTRATOR, true, 5L, finalMessage));
        assertEquals(ExceptionMessage.USER_ALREADY_BANNED, e.getMessage());

        clearInvocations(authDao);
        clearInvocations(banRecordDao);
        userService.banUser(7L, UserConstant.ADMINISTRATOR, false, 5L, null);
        verify(authDao).saveAuth(argThat(item -> item.getUserId() == 5L && !item.getBanned()));
        verify(banRecordDao).completeRecord(eq(5L), eq(7L), any());

        e = assertThrows(ServerException.class, () -> userService.banUser(7L, UserConstant.NORMAL_USER, true, 5L, finalMessage));
        assertEquals(ExceptionMessage.FORBIDDEN, e.getMessage());

        while (message.length() <= Constraints.BAN_REASON_MAX)
            message += message;
        String finalMessage1 = message;
        e = assertThrows(ServerException.class, ()->userService.banUser(7L, UserConstant.ADMINISTRATOR, true, 5L, finalMessage1));
        assertEquals(e.getMessage(), ExceptionMessage.INVALID_PARAMETER);
    }

    @Test
    public void setRole() throws ServerException {
        User user = new User();
        doReturn(user).when(userDao).getUserInfoById(5L);
        doNothing().when(userDao).saveUserInfo(any());
        userService.setUserRole(5L, UserConstant.ADMINISTRATOR);
        verify(userDao).saveUserInfo(argThat(item -> item.getRole().equals(UserConstant.ADMINISTRATOR)));
        clearInvocations(userDao);
        userService.setUserRole(5L, UserConstant.NORMAL_USER);
        verify(userDao).saveUserInfo(argThat(item -> item.getRole().equals(UserConstant.NORMAL_USER)));
    }

    @Test
    public void getBanRecords() throws ServerException {
        String nickname = "admin";
        String reason = "banned reason";
        List<BanRecord> list = new ArrayList<>();
        BanRecord record = new BanRecord();
        record.setReason(reason);
        record.setBannedBy(7L);
        record.setBannedTime(new Date());
        record.setRecoveredBy(7L);
        record.setRecoveredTime(new Date());
        list.add(record);
        doReturn(list).when(banRecordDao).getBanRecords(5L);
        User admin = new User();
        admin.setNickname(nickname);
        doReturn(admin).when(userDao).getUserInfoById(7L);
        doReturn(admin).when(userDao).getUserById(7L);
        assertEquals(reason, userService.getBanRecords(5L).get(0).getBannedMessage());

        doThrow(new ServerException("")).when(userDao).getUserInfoById(any());
        doThrow(new ServerException("")).when(userDao).getUserById(any());
        assertThrows(ServerException.class, () -> userService.getBanRecords(5L));
    }

    @Test
    public void getPosts() throws ServerException {
        String interest = "interest";
        User user = new User();
        user.setUserId(3L);
        List<Post> list = new ArrayList<>();
        Post post = new Post(), origin = new Post();
        post.setPostId(5L);
        post.setUser(user);
        post.setTime(new Date());
        post.setForwardedFrom(7L);
        post.setDeleted(false);
        post.setCancelled(false);
        origin.setPostId(7L);
        origin.setDeleted(true);
        list.add(post);
        doReturn(list).when(postDao).getUserPosts(3L, -1L, 10L, false);
        doReturn(false).when(userDao).getLiked(any(), any());
        doReturn(origin).when(postDao).getPostById(7L);
        doNothing().when(visitDao).removeVisit(any(), any());
        doNothing().when(visitDao).addVisit(any());
        assertEquals(3L, userService.getPosts(5L, 3L, -1L, 10L).get(0).getUserId());
        verify(visitDao).removeVisit(5L, 3L);
        verify(visitDao).addVisit(argThat(item -> item.getVisitor() == 5L && item.getVisited() == 3L));

        doThrow(new ServerException("")).when(postDao).getPostById(7L);
        assertEquals(-1L, userService.getPosts(5L, 3L, -1L, 10L).get(0).getForwardedFrom());

        List<Interest> interestList = new ArrayList<>();
        Interest item = new Interest();
        item.setTitle(interest);
        interestList.add(item);
        post.setForwardedFrom(null);
        post.setInterests(interestList);
        Transport.PostListItem result = userService.getPosts(5L, 3L, -1L, 10L).get(0);
        assertTrue(result.getForwardedFrom() == -2L && result.getInterests().get(0).equals(interest));

        doReturn(list).when(postDao).getAllPosts(any(), any());
        userService.getAllPosts(5L, -1L, 10L);
        verify(postDao).getAllPosts(-1L, 10L);

        doReturn(list).when(postDao).getDeletedPosts(any(), any());
        userService.getDeletedPosts(5L, -1L, 10L);
        verify(postDao).getDeletedPosts(-1L, 10L);

        doReturn(list).when(postDao).getHomePage(any(), any(), any());
        userService.getHomePage(5L, -1L, 10L);
        verify(postDao).getHomePage(5L, -1L, 10L);

        doReturn(list).when(postDao).searchPostsByContent(any(), any(), any(), any());
        userService.searchPostsByContent(5L, "text", false, -1L, 10L);
        verify(postDao).searchPostsByContent("text", false, -1L, 10L);

        assertThrows(ServerException.class, ()->userService.getPosts(5L, 3L, -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.getHomePage(5L, -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.getAllPosts(5L, -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.getDeletedPosts(5L, -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.searchPostsByContent(5L, "text", false, -1L, Constraints.COUNT_MAX + 1L));
    }

    @Test
    public void follow() throws ServerException {
        User user1 = new User(), user2 = new User();
        user1.setUserId(5L);
        user1.setFollowCount(6L);
        user2.setUserId(7L);
        user2.setFollowerCount(100L);
        doReturn(user1).when(userDao).getUserById(5L);
        doReturn(user1).when(userDao).getUserInfoById(5L);
        doReturn(user2).when(userDao).getUserById(7L);
        doReturn(user2).when(userDao).getUserInfoById(7L);
        doNothing().when(userDao).saveUserInfo(any());
        doReturn(true).when(userDao).getFollowed(5L, 7L);
        userService.follow(5L, 7L, false);
        verify(userDao).unFollow(5L, 7L);
        verify(userDao).saveUserInfo(argThat(item -> item.getUserId() == 7L && item.getFollowerCount() == 99L));

        clearInvocations(userDao);
        doReturn(false).when(userDao).getFollowed(5L, 7L);
        userService.follow(5L, 7L, true);
        verify(userDao).follow(5L, 7L);
        verify(userDao).saveUserInfo(argThat(item -> item.getUserId() == 7L && item.getFollowerCount() == 100L));
    }

    @Test
    public void getFollowsAndFollowers() throws ServerException {
        String profile = "profile";

        List<User> list = new ArrayList<>();
        User user = new User();
        user.setProfile(profile);
        list.add(user);
        doReturn(list).when(userDao).getFollows(5L, -1L, 10L);
        assertEquals(profile, userService.getFollows(5L, -1L, 10L).get(0).getProfile());

        doReturn(list).when(userDao).getFollowers(5L, -1L, 10L);
        assertEquals(profile, userService.getFollowers(5L, -1L, 10L).get(0).getProfile());

        assertThrows(ServerException.class, ()->userService.getFollows(5L, -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.getFollowers(5L, -1L, Constraints.COUNT_MAX + 1L));
    }

    @Test
    public void getVisitors() throws ServerException {
        String nickname = "nickname";

        List<Visit> list = new ArrayList<>();
        Visit visit = new Visit();
        visit.setTime(new Date());
        visit.setVisitor(7L);
        list.add(visit);
        doReturn(list).when(visitDao).getVisitors(5L);
        User user = new User();
        user.setNickname(nickname);
        doReturn(user).when(userDao).getUserInfoById(7L);
        doReturn(user).when(userDao).getUserById(7L);
        assertEquals(nickname, userService.getVisitors(5L).get(0).getNickname());
    }

    @Test
    public void like() throws ServerException {
        doReturn(true).when(userDao).getLiked(any(), any());
        doReturn(true).when(userDao).getLikedRemark(any(), any());
        userService.likePost(5L, 7L, true);
        userService.likeRemark(5L, 7L, true);
        verifyNoInteractions(postDao);

        Post post = new Post();
        post.setPostId(7L);
        post.setLikeCount(100L);
        doReturn(post).when(postDao).getPostInfoById(7L);
        doNothing().when(postDao).savePostInfo(any());
        doNothing().when(userDao).dislikePost(any(), any());
        userService.likePost(5L, 7L, false);
        verify(postDao).savePostInfo(argThat(item -> item.getPostId() == 7L && item.getLikeCount() == 99L));
        verify(userDao).dislikePost(5L, 7L);

        RemarkItem remark = new RemarkItem();
        remark.setLikeCount(100L);
        remark.setId(7L);
        doReturn(remark).when(remarkItemDao).getRemarkById(7L);
        doReturn(null).when(remarkItemDao).saveRemark(any());
        doNothing().when(userDao).dislikeRemark(any(), any());
        userService.likeRemark(5L, 7L, false);
        verify(remarkItemDao).saveRemark(argThat(item -> item.getId() == 7L && item.getLikeCount() == 99L));
        verify(userDao).dislikeRemark(5L, 7L);

        clearInvocations(postDao);
        clearInvocations(userDao);
        clearInvocations(remarkItemDao);
        doReturn(false).when(userDao).getLiked(any(), any());
        doReturn(false).when(userDao).getLikedRemark(any(), any());
        doNothing().when(userDao).likePost(any(), any());
        doNothing().when(userDao).likeRemark(any(), any());
        userService.likePost(5L, 7L, true);
        userService.likeRemark(5L, 7L, true);
        verify(postDao).savePostInfo(argThat(item -> item.getLikeCount() == 100L));
        verify(userDao).likePost(5L, 7L);
        verify(remarkItemDao).saveRemark(argThat(item -> item.getLikeCount() == 100L));
        verify(userDao).likeRemark(5L, 7L);

        doThrow(new EntityNotFoundException()).when(postDao).getPostInfoById(any());
        Exception e = assertThrows(ServerException.class, () -> userService.likePost(5L, 7L, true));
        assertEquals(ExceptionMessage.POST_NOT_FOUND, e.getMessage());
    }

    @Test
    public void searchInterests() {
        String title = "database";
        List<String> list = new ArrayList<>();
        doReturn(list).when(interestDao).searchInterest(title);
        assertSame(list, userService.searchInterest(title));
    }

    @Test
    public void searchUser() throws ServerException {
        String name = "nickname";
        String title = "database";

        List<User> list = new ArrayList<>();
        User user = new User();
        user.setNickname(name);
        list.add(user);
        doReturn(list).when(userDao).searchUser(name, -1L, 10L);
        assertEquals(name, userService.searchUser(name, -1L, 10L).get(0).getNickname());

        doReturn(list).when(userDao).searchUserByInterest(title, -1L, 10L);
        assertEquals(name, userService.searchUserByInterest(title, -1L, 10L).get(0).getNickname());

        assertThrows(ServerException.class, ()->userService.searchUserByInterest("interest", -1L, Constraints.COUNT_MAX + 1L));
        assertThrows(ServerException.class, ()->userService.searchUser("name", -1L, Constraints.COUNT_MAX + 1L));
    }

    @Test
    public void popular() {
        class ResultTuple implements Tuple {
            @Override
            public <X> X get(TupleElement<X> tupleElement) {
                return null;
            }

            @Override
            public <X> X get(String s, Class<X> aClass) {
                return null;
            }

            @Override
            public Object get(String s) {
                return null;
            }

            @Override
            public <X> X get(int i, Class<X> aClass) {
                return null;
            }

            @Override
            public Object get(int i) {
                if (i == 0)
                    return "database";
                return 100L;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public List<TupleElement<?>> getElements() {
                return null;
            }
        }
        List<Tuple> list = new ArrayList<>();
        list.add(new ResultTuple());
        doReturn(list).when(userDao).popularInterests();
        assertEquals("database", userService.popularInterests().get(0).getInterest());
    }

    @Test
    public void popularUser() throws ServerException {
        List<User> list = new ArrayList<>();
        User user = new User();
        user.setUserId(5L);
        user.setNickname("nick");
        user.setIcon("icon");
        list.add(user);
        doReturn(list).when(userDao).getPopular();
        assertEquals("icon", userService.getPopular().get(0).getIcon());
    }

    @Test
    public void flush() {
        doNothing().when(userDao).flush();
        doNothing().when(authDao).flush();
        doNothing().when(interestDao).flush();
        doNothing().when(visitDao).flush();
        userService.flush();
        verify(userDao).flush();
        verify(authDao).flush();
        verify(interestDao).flush();
        verify(visitDao).flush();
    }
}
