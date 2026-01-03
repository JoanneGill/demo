package com.example.demo.Mapper;


import com.example.demo.Data.EC;
import org.apache.ibatis.annotations.*;

@Mapper
public interface VersionMapper {


    @Insert({"insert into ecversion(download_url,version,dialog,msg,`force`) values(#{download_url},#{version},#{dialog},#{msg},#{force})"})
    public Boolean setVersion(EC ec) ;

    @Select({"select * from ecversion  where ecTrueVersion = #{ecTrueVersion} order by id desc limit 1; "})
    public EC getNewVersion(String ecTrueVersion);
//    @Delete({"delete ECVersion where  "})
//    public boolean deleteAllVersion();
}
