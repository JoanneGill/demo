package com.example.demo.Mapper;


import com.example.demo.Data.ExchangeIntegral;
import com.example.demo.Data.User;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select({"select cardNo,  totalIntegral, email, freezeIntegral, realName, tempIntegral, redeemedIntegral," +
            "                  qrUrl,qrUrlZFB,qrUrlOY ,availableIntegral, permissions,state  from user where cardNo=#{cardNo}"})
    User selectMyInfoByCardNo(@Param("cardNo") String cardNo);

    @Select({"select cardNo,  totalIntegral, email, freezeIntegral, realName, tempIntegral, redeemedIntegral," +
            "                  qrUrl, availableIntegral,permissions,state from user where cardNo=#{cardNo} and password=#{password} "})
    User selectMyInfo(@Param("cardNo") String cardNo,@Param("password") String password);


    Boolean setMyInfo(@Param("user") User user);

    @Update({"update user set  availableIntegral = availableIntegral-#{integral} where cardNo=#{cardNo}"})
    Boolean exchangeIntegral(@Param("cardNo") String cardNo,@Param("integral") Long integral);


    @Select({"select * from  exchangeintegral where cardNo=#{cardNo}"})
    List<ExchangeIntegral> selectMyExchangeIntegral(@Param("cardNo") String cardNo);


    Boolean addUser(@Param("user") User user);

    @Select({"select cardNo,  totalIntegral, email, freezeIntegral, realName, tempIntegral, redeemedIntegral," +
            "qrUrl, availableIntegral, state from user where state= #{user.state}" })
    List<User> selectUserList(@Param("user") User user);


    void updataTempIntegralEveryday();

    @Select({"select sum(tempIntegral) from user "})
    Long selectTodayAllIntegral();

    @Update({"update  user set tempIntegral= #{t} where cardNo=#{cardNo}"})
    boolean changeTempIntegral(@Param("cardNo") String cardNo,@Param("t") Long tempIntegral);

    @Update({"update  user set availableIntegral= #{t} where cardNo=#{cardNo}"})
    boolean changeIntegral(@Param("cardNo") String cardNo,@Param("t") Long tempIntegral);

    @Insert({"insert into adminsettempintegral(cardNo,tempIntegral,changeTime) values(#{cardNo},#{tempIntegral},now())"})
    boolean addChangeTempIntegral(String cardNo, Long tempIntegral);

    @Update({"update user set state = #{state} where cardNo = #{cardNo}"})
    Boolean setUserState(@Param("cardNo") String cardNo,@Param("state") Integer state);

    @Update({"update user set availableIntegral =availableIntegral+ #{integral} where cardNo=#{cardNo}"})
    boolean addAvailableIntegral(@Param("cardNo") String cardNo,@Param("integral") Long integral);

    @Update({"update user set password = #{password} where cardNo=#{cardNo}"})
    boolean changePassword(@Param("cardNo") String cardNo,@Param("password") String password);
}
