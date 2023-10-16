package com.magicrepokit.auth.service;

import com.magicrepokit.auth.constant.MRKI18NEnum;
import com.magicrepokit.auth.constant.MRKUserTypeEnum;
import com.magicrepokit.auth.entity.AuthAccessToken;
import com.magicrepokit.common.api.R;
import com.magicrepokit.common.api.ResultCode;
import com.magicrepokit.log.exceotion.ServiceException;
import com.magicrepokit.user.entity.AuthClient;
import com.magicrepokit.user.entity.User;
import com.magicrepokit.user.feign.SystemClient;
import com.magicrepokit.user.vo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MRKAuthGrantServiceImpl implements MRKAuthGrantService {
    @Autowired
    private MRKAuthTokenService mrkAuthTokenService;
    @Autowired
    private SystemClient systemClient;

    /**
     * 校验客户端信息
     *
     * @param clientId
     * @param clientSecret
     * @return
     */
    @Override
    public AuthClient validOAuthClient(String clientId, String clientSecret,String grantType,List<String> scopes,String redirectUri) {
        //校验客户端
        R<AuthClient> oAuth2ClientR = systemClient.validOAuthClientFromCache(clientId, clientSecret,
                grantType, scopes, redirectUri);
        if(!oAuth2ClientR.isSuccess()){
            throw new ServiceException(oAuth2ClientR.getMsg());
        }
        AuthClient client = oAuth2ClientR.getData();
        if(client==null){
            throw new ServiceException(MRKI18NEnum.NOT_FOUND_CLIENT.getMessage());
        }
        return client;
    }

    /**
     * 密码模式
     *
     * @param username 账号
     * @param password 密码
     * @param clientId 客户端编号
     * @param scopes 授权范围
     * @return
     */
    @Override
    public AuthAccessToken grantPassword(String username, String password, String clientId, List<String> scopes,String userType) {
        //获取用户
        R<UserInfo> userInfoR = systemClient.userInfo(username);
        if(!userInfoR.isSuccess()||userInfoR.getData()==null){
            throw new ServiceException(ResultCode.UN_AUTHORIZED, MRKI18NEnum.USER_NOT_FOUND.getMessage());
        }
        UserInfo userInfo = userInfoR.getData();
        //校验用户类型
        MRKUserTypeEnum userTypeEnum = MRKUserTypeEnum.getByUserType(userType);
        User user = userInfo.getUser();
        if (!user.getUserType().contains(userTypeEnum.getCode())) {
            throw new ServiceException(ResultCode.UN_AUTHORIZED, MRKI18NEnum.UNKNOWN_USER_TYPE.getMessage());
        }
        //获取客户端
        R<AuthClient> authClientR = systemClient.authClientInfo(clientId);
        AuthClient client = authClientR.getData();
        if(!authClientR.isSuccess()||client==null){
            throw new ServiceException(MRKI18NEnum.NOT_FOUND_CLIENT.getMessage());
        }
        //创造令牌

        return mrkAuthTokenService.createAccessToken(userInfo, client, scopes,userType);
    }

    /**
     * 授权码模式
     *
     * @param clientId 客户端编号
     * @param code 授权码
     * @param redirectUri 重定向 URI
     * @param state 状态
     * @return 令牌信息
     */
    @Override
    public AuthAccessToken grantAuthorizationCodeForAccessToken(String clientId, String code, String redirectUri, String state,String userType) {
        return null;
    }

    /**
     * 刷新模式
     *
     * @param refreshToken 刷新令牌
     * @param clientId 客户端编号
     * @return 令牌信息
     */
    @Override
    public AuthAccessToken grantRefreshToken(String refreshToken, String clientId,String userType) {
        return null;
    }

    /**
     * 获取授权码
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @param clientId 客户端编号
     * @param scopes 授权范围
     * @param redirectUri 重定向 URI
     * @param state 状态
     * @return 令牌信息
     */
    @Override
    public String grantAuthorizationCodeForCode(Long userId, Integer userType, String clientId, List<String> scopes, String redirectUri, String state) {
        return null;
    }
}
