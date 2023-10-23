package com.magicrepokit.system.service.impl;



import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.magicrepokit.common.utils.WebUtil;
import com.magicrepokit.jwt.constant.JWTConstant;
import com.magicrepokit.jwt.utils.JWTUtil;
import com.magicrepokit.log.exceotion.ServiceException;
import com.magicrepokit.social.constant.SocialTypeEnum;
import com.magicrepokit.social.factory.MRKAuthRequestFactory;
import com.magicrepokit.system.constant.SystemConstant;
import com.magicrepokit.system.constant.SystemResultCode;
import com.magicrepokit.system.constant.SystemUserStatus;
import com.magicrepokit.system.entity.dto.AuthLoginDTO;
import com.magicrepokit.system.entity.dto.AuthSocialLoginDTO;
import com.magicrepokit.system.entity.vo.AuthTokenVO;
import com.magicrepokit.system.service.IAuthService;
import com.magicrepokit.system.service.IUserService;
import com.magicrepokit.system.entity.vo.UserInfo;
import com.xingyuv.jushauth.model.AuthCallback;
import com.xingyuv.jushauth.model.AuthResponse;
import com.xingyuv.jushauth.model.AuthUser;
import com.xingyuv.jushauth.request.AuthRequest;
import com.xingyuv.jushauth.utils.AuthStateUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 登录授权服务
 *
 * @author AuroraPixel
 * @github https://github.com/AuroraPixel
 */
@Service
@Slf4j
public class AuthServiceImpl implements IAuthService {
    @Autowired
    private IUserService userService;
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private MRKAuthRequestFactory mrkAuthRequestFactory;
    @Autowired
    private RestTemplate restTemplate;
    @Value("${mrk.auth.local.client-id}")
    private String clientId;
    @Value("${mrk.auth.local.client-secret}")
    private String clientSecret;

    /**
     *  登录
     *
     * @param authLoginDTO 登录信息
     * @return 登录信息
     */
    @Override
    public AuthTokenVO login(AuthLoginDTO authLoginDTO) {
        //校验用户
        UserInfo authenticate = authenticate(authLoginDTO.getUsername(), authLoginDTO.getPassword());
        //oauth登录获取令牌

        return remoteTokenService(JWTConstant.PASSWORD, clientId, clientSecret, authLoginDTO.getUsername(), authLoginDTO.getPassword(), null);
    }

    /**
     * 刷新token
     *
     * @param refreshToken 刷新token
     * @return 令牌信息
     */
    @Override
    public AuthTokenVO refreshToken(String refreshToken) {
        //解析token
        Claims claims = JWTUtil.parseJWT(refreshToken);
        if(claims==null){
            throw new ServiceException(SystemResultCode.REFRESH_TOKEN_FAIL);
        }
        String userId = String.valueOf(claims.get(JWTConstant.USER_ID));
        String refreshTokenRedis = JWTUtil.getRefreshToken(Long.valueOf(userId), getUserType());
        if(refreshTokenRedis==null||!refreshTokenRedis.equals(refreshToken)){
            throw new ServiceException(SystemResultCode.REFRESH_TOKEN_FAIL);
        }
        return remoteTokenService(JWTConstant.REFRESH_TOKEN,clientId,clientSecret,null,null,refreshToken);
    }

    /**
     *  获取三方认证请求地址
     *
     * @param type 三方平台 10：github 20:google 30:gitee
     * @param redirectUri 本系统回掉地址
     * @return 三方请求地址
     */
    @Override
    public String socialLoginRedirect(Integer type, String redirectUri) {
        SocialTypeEnum socialTypeEnum = SocialTypeEnum.valueOfType(type);
        if(socialTypeEnum==null){
            throw new ServiceException(SystemResultCode.NOT_FOUND_SOCIAL_TYPE);
        }
        // 获得对应的 AuthRequest 实现
        AuthRequest authRequest = mrkAuthRequestFactory.get(socialTypeEnum.getSource());
        // 生成跳转地址
        String authorizeUri = authRequest.authorize(AuthStateUtils.createState());
        return WebUtil.replaceUrlQuery(authorizeUri, "redirect_uri", redirectUri);
    }

    /**
     * 三方快捷登录换取本系统token
     *
     * @param authSocialLoginDTO 三方code相关信息
     * @return 令牌信息
     */
    @Override
    public AuthTokenVO socialLogin(AuthSocialLoginDTO authSocialLoginDTO) {
        Integer type = authSocialLoginDTO.getType();
        String state = authSocialLoginDTO.getState();
        String code = authSocialLoginDTO.getCode();
        //1.从数据库中获取

        //2.远程三方获得社交用户信息
        AuthUser socialUser = getSocialUser(type, code, state);
        //3.保存数据库

        //4.判断是否与本系统用户绑定

        //5.若绑定返回token

        return null;
    }

    /**
     * 社交平台获取用户信息
     *
     * @param type 社交平台类型
     * @param code 授权码
     * @param state 状态码
     * @return 社交平台用户
     */
    private AuthUser getSocialUser(Integer type, String code, String state) {
        SocialTypeEnum socialTypeEnum = SocialTypeEnum.valueOfType(type);
        if(socialTypeEnum==null){
            throw new ServiceException(SystemResultCode.NOT_FOUND_SOCIAL_TYPE);
        }
        AuthRequest authRequest = mrkAuthRequestFactory.get(socialTypeEnum.getSource());
        AuthCallback authCallback = AuthCallback.builder().code(code).state(state).build();
        AuthResponse<?> authResponse = authRequest.login(authCallback);
        log.info("[getAuthUser][请求社交平台 type({}) request({}) response({})]", type,
                JSONUtil.toJsonStr(authCallback), JSONUtil.toJsonStr(authResponse));
        if(!authResponse.ok()){
            throw new ServiceException(SystemResultCode.SOCIAL_USER_AUTH_FAILURE,authResponse.getMsg());
        }
        return (AuthUser) authResponse.getData();
    }


    /**
     * 认证中心获取令牌
     *
     * @param grantType 授权类型
     * @param clientId 客户端id
     * @param clientSecret 客户端密码
     * @param username 用户名
     * @param password 密码
     * @param refreshToken 刷新token
     * @return 令牌信息
     */
    private AuthTokenVO remoteTokenService(String grantType,String clientId,String clientSecret,String username,String password,String refreshToken){
        String userType = getUserType();
        //负载获取远程服务
        ServiceInstance serviceInstance = loadBalancerClient.choose(SystemConstant.REMOTE_AUTH_NAME);
        if(serviceInstance==null){
            throw new ServiceException(SystemResultCode.NOT_FOUND_SERVICE,SystemConstant.REMOTE_AUTH_NAME);
        }
        //获取远程地址
        String path = serviceInstance.getUri().toString()+SystemConstant.OAUTH_TOKEN_URL;
        //定义body
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(JWTConstant.GRANT_TYPE,grantType);
        formData.add(JWTConstant.USERNAME,username);
        formData.add(JWTConstant.PASSWORD,password);
        formData.add(JWTConstant.REFRESH_TOKEN,refreshToken);
        String queryParams = WebUtil.buildQueryParams(formData);
        String urlWithParams = path + queryParams;

        //定义请求头
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add(JWTConstant.AUTHORIZATION, JWTUtil.getAuthorization(clientId,clientSecret));
        header.add(JWTConstant.USER_TYPE,userType);
        //远程请求
        Map<String,Object> remoteResult;
        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, new HttpEntity<>(null, header), Map.class);
            remoteResult = responseEntity.getBody();
        } catch (Exception e){
            log.error(e.getMessage());
            throw new ServiceException(SystemResultCode.REMOTE_SERVICE_ERROR,SystemConstant.REMOTE_AUTH_NAME);
        }
        if(remoteResult == null || remoteResult.get(JWTConstant.ACCESS_TOKEN) == null || remoteResult.get(JWTConstant.REFRESH_TOKEN) == null || remoteResult.get(JWTConstant.JTI) == null) {
            //jti是jwt令牌的唯一标识作为用户身份令牌
            throw new ServiceException(SystemResultCode.CREATE_JWT_FAILED);
        }
        return BeanUtil.toBean(remoteResult, AuthTokenVO.class);
    }

    /**
     * 请求头获取用户类别
     *
     * @return userType
     */
    private static String getUserType() {
        HttpServletRequest request = WebUtil.getRequest();
        String userType = request.getHeader(JWTConstant.USER_TYPE);
        if(userType==null){
            throw new ServiceException(SystemResultCode.NOT_FOUND_USER_TYPE);
        }
        return userType;
    }


    /**
     * 验证账户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return
     */
    public UserInfo authenticate(String username,String password){
        //查询用户信息
        UserInfo userInfo = userService.userInfo(username);
        if(ObjectUtil.isEmpty(userInfo)){
            throw new ServiceException(SystemResultCode.NOT_FOUND_USER);
        }
        //校验密码
        if (!userService.isPasswordMatch(password,userInfo.getUser().getPassword())) {
            throw new ServiceException(SystemResultCode.NOT_FOUND_USER);
        }
        //判断是否激活
        if(userInfo.getUser().getStatus()== SystemUserStatus.Disabled.getCode()){
            throw new ServiceException(SystemResultCode.DISABLED_USER);
        }
        return userInfo;
    }
}
