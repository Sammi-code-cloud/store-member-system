package com.bama.store;
import com.bama.store.service.*;
import com.bama.store.dto.LoginResponse;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class DesktopWechatLoginTest {
 WechatClient wx;JdbcTemplate jdbc;AuthService auth;StoreAvailability stores;DesktopWechatLogin service;
 @BeforeEach void setup(){wx=mock(WechatClient.class);jdbc=mock(JdbcTemplate.class);auth=mock(AuthService.class);stores=mock(StoreAvailability.class);service=new DesktopWechatLogin(wx,jdbc,auth,stores);when(wx.desktopLoginCode(anyString())).thenReturn("data:image/png;base64,test");when(wx.exchange("MINI","fresh")).thenReturn(new WechatClient.Identity("app","openid",""));when(jdbc.queryForList(anyString(),eq(Long.class),anyString())).thenReturn(List.of(7L));LoginResponse r=new LoginResponse();r.setStaffId(7L);r.setStoreId(1L);r.setToken("test-token");r.setPermissions(Set.of("dashboard:view"));when(auth.loginById(7L)).thenReturn(r);}
 @Test void onlyBrowserSecretCanReceiveConfirmedLoginOnce(){var c=service.create("ip");String t=(String)c.get("ticket"),s=(String)c.get("secret");assertThat(service.poll(t,s).get("status")).isEqualTo("WAITING");service.confirm(t,"fresh");assertThatThrownBy(()->service.poll(t,t)).hasMessageContaining("无效");assertThat(service.poll(t,s).get("status")).isEqualTo("CONFIRMED");assertThatThrownBy(()->service.poll(t,s)).hasMessageContaining("过期");verify(stores,times(2)).requireActive(1L);}
 @Test void unboundWechatCannotApprove(){when(jdbc.queryForList(anyString(),eq(Long.class),anyString())).thenReturn(List.of());var c=service.create("ip");assertThatThrownBy(()->service.confirm((String)c.get("ticket"),"fresh")).hasMessageContaining("未绑定");verifyNoInteractions(auth);}
 @Test void cancelInvalidatesCodeAndCannotBeForged(){var c=service.create("ip");String t=(String)c.get("ticket"),s=(String)c.get("secret");assertThatThrownBy(()->service.cancel(t,"wrong"));service.cancel(t,s);assertThatThrownBy(()->service.confirm(t,"fresh")).hasMessageContaining("过期");verify(wx,never()).exchange(anyString(),anyString());}
 @Test void disabledEmployeeAtCollectionIsRejected(){var c=service.create("ip");service.confirm((String)c.get("ticket"),"fresh");when(auth.loginById(7L)).thenThrow(new com.bama.store.common.BusinessException("员工已停用"));assertThatThrownBy(()->service.poll((String)c.get("ticket"),(String)c.get("secret"))).hasMessageContaining("停用");}
 @Test void repeatedConfirmationAndExcessCreationAreRejected(){var c=service.create("ip");service.confirm((String)c.get("ticket"),"fresh");assertThatThrownBy(()->service.confirm((String)c.get("ticket"),"fresh"));assertThatThrownBy(()->service.create("ip")).hasMessageContaining("频繁");}
 @Test void expiredCodeCannotBeConfirmed() throws Exception {var c=service.create("ip");var f=DesktopWechatLogin.class.getDeclaredField("entries");f.setAccessible(true);var entries=(Map<?,?>)f.get(service);var e=entries.get(c.get("ticket"));var expiry=e.getClass().getDeclaredField("expires");expiry.setAccessible(true);expiry.setLong(e,0);assertThatThrownBy(()->service.confirm((String)c.get("ticket"),"fresh")).hasMessageContaining("过期");}
}
