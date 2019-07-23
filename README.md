# hive-start

## 官网：https://hive.apache.org/
## 参考教程： https://www.yiibai.com/hive/


### 搭建hive 并集成hadoop，以mysql作为元数据库（centos7以后不支持mysql，支持mariadb，完全兼容mariadb）

* 安装参考连接：http://blog.zhangshun.net/archives/401  https://blog.csdn.net/qq_37106028/article/details/78247727


### 安装mysql遇到的问题

* (1) 开始我按照mysql 下载yum源，安装此 https://blog.csdn.net/qq_38591756/article/details/82958333 操作。
  * 地址： https://dev.mysql.com/downloads/file/?id=484922  看到 No thanks, just start my download. 右键赋值链接地址；
  ```
  * wget https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
  ```
  * 执行上述命令，遇到错误：解析主机失败 ping baidu.com也失败 说明虚拟机网络模式设置和dns配置有：https://blog.csdn.net/bob_666/article/details/81412242 安装这里的说明进行修改,以后可以。
* (2)安装yum源：
```
yum localinstall mysql80-community-release-el7-1.noarch.rpm
```
* (3)检查安装是否成功： 
```
yum repolist enabled | grep "mysql.*-community.*"
```
* (4)yum安装： 
```
yum install mysql-community-server
```
* (5)启动musql: 
```
systemctl start mysqld 或者 service mysqld start

# 你会发现启动失败：
Failed to start mysqld.service: Unit not found原因就是centos7 放弃mysql； 可以了解下：https://www.zhihu.com/question/41832866
```
  
* (6)赋予用户root权限: 
  ```
  grant all privileges on *.* to root@”sun.com” identified by “123456”;   
  #root登录mysql的用户名，sun.com你虚拟机的ip 123456 是你的mysql密码
  ```
### 一、安装mariadb

* 经过上述安装mysql 踩过的坑之后，我们使用mariadb 

* 1.安装:
```
yum install -y mariadb-server
```
* 2.启动maria DB服务:
```
systemctl start mariadb.service
(说明：CentOS 7.x开始，CentOS开始使用systemd服务来代替daemon，原来管理系统启动和管理系统服务的相关命令全部由systemctl命令来代替。)
```
* 3.将mariadb服务添加至开机自启动：
```
systemctl enable mariadb.service
```
* 4.进入mariadb 
```
mysql -u root -p 回车 密码为空
```
* 5.给root用户全部权限
```
grant all privileges on *.* to root@”sun.com” identified by “123456”;   
 #root登录mysql的用户名，sun.com你虚拟机的ip 123456 是你的mysql密码
```
* 6.刷新授权表
```
flush privileges
```
* 7.修改密码，如果需要可以进行修改字符集
```
set password for root@localhost = password('123456')
# 注意修改的密码，在配置hive-site.xml 会用到，别忘记
```

### 二、安装hive

* 参考链接： https://www.yiibai.com/hive/hive_installation.html  首先需要安装好jdk 和 hadoop 并配置好环境变量
* 可以去官网下载和你之前安装hadoop版本对应的hive版本 （我这里用的是hadoop2.7.7 hive2.3.5） 地址：https://hive.apache.org/downloads.html
* （1）我们本地下载解压，拷贝到虚拟机（因为虚拟机直接下载有点慢）
* （2）将文件复制或者移动到/usr/local/hive目录，切换root 用户  su -
```
mv apache-hive-0.14.0-bin /usr/local/hive
```
* （3）配置Hive环境
```
vi ~/.bashrc （这个文件是针对当前用户）或者 修改/etc/profile 全局的
# 可以设置Hive环境，通过附加以下行到〜/.bashrc文件中：
export HIVE_HOME=/usr/local/hive
export PATH=$PATH:$HIVE_HOME/bin
export CLASSPATH=$CLASSPATH:/usr/local/hadoop/lib/*:.
export CLASSPATH=$CLASSPATH:/usr/local/hive/lib/*:.

# 下面的命令是用来执行加载〜/.bashrc文件。
source ~/.bashrc
```
* （4）配置Hive
```  
# 配置Hive用于Hadoop环境中，需要编辑hive-env.sh文件，该文件放置在 $HIVE_HOME/conf目录。下面的命令重定向到Hive config文件夹并复制模板文件：
cd $HIVE_HOME/conf
cp hive-env.sh.template hive-env.sh
# 通过编辑hive-env.sh文件添加以下行,指定hadoop的安装目录：
export HADOOP_HOME=/usr/local/hadoop
```
* （5）配置Hive的Metastore （我们使用mysql作为元数据库，不用链接提供的）
```
# 配置Metastore意味着，指定要Hive的数据库存储。可以通过编辑hive-site.xml 文件，在$HIVE_HOME/conf目录下可以做到这一点。
$ cd $HIVE_HOME/conf
$ cp hive-default.xml.template hive-site.xml

vi hive-site.xml

<!--新增配置-->
<property>
        <name>javax.jdo.option.ConnectionUserName</name>
        <value>root</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionPassword</name>
        <value>123456</value>
    </property>
   <property>
        <name>javax.jdo.option.ConnectionURL</name>mysql
        <value>jdbc:mysql://192.168.2.31:3306/hive?createDatabaseIfNotExist</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionDriverName</name>
        <value>org.mariadb.jdbc.Driver</value>
    </property>
 <property>
    <name>datanucleus.schema.autoCreateAll</name>
    <value>true</value>
  </property>
<!--结束-->
# 注意，要删除原来配置文件存在的相同配置项，可以通过 :/xxx 来查找，如果不去掉，hive会默认忽略你得配置，配置文件刚开始的注释警告已经给出提示。
# jdbc:mysql://192.168.2.31:3306/hive?createDatabaseIfNotExist 这样配置，我们启动hive时候，会自动再mariadb给我们创建hive数据库。
```

* （6）添加mariadb的驱动jar包：
  * 下载：https://downloads.mariadb.com/Connectors/java/connector-java-2.1.2/mariadb-java-client-2.1.2.jar 复制到hive的lib目录下

* （7）运行Hive之前，需要创建/tmp文件夹在HDFS独立的Hive文件夹。在这里使用/user/hive/warehouse文件夹。需要给这些新创建的文件夹写权限
```
# 现在，设置它们在HDFS验证Hive之前。使用下面的命令：
  hdfs dfs -mkdir /tmp 
  hdfs dfs -mkdir /user/hive/warehouse
  hdfs dfs -chmod g+w /tmp 
  hdfs dfs -chmod g+w /user/hive/warehouse
```
* （8）启动hive  再hive目录下  bin/hive

### 各种报错
* （1）
```
hive> show tables;
FAILED: SemanticException org.apache.hadoop.hive.ql.metadata.HiveException: java.lang.RuntimeException: Unable to instantiate org.apache.hadoop.hive.ql.metadata.SessionHiveMetaStoreClient

# 需要初始化数据库 ：
[root@sun conf]# schematool -dbType mysql -initSchema
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/usr/local/hive/lib/log4j-slf4j-impl-2.6.2.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/usr/local/hadoop/share/hadoop/common/lib/slf4j-log4j12-1.7.10.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.apache.logging.slf4j.Log4jLoggerFactory]
Metastore connection URL:	 jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :	 org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:	 APP
Starting metastore schema initialization to 2.3.0
Initialization script hive-schema-2.3.0.mysql.sql
Error: Syntax error: Encountered "<EOF>" at line 1, column 64. (state=42X01,code=30000)
org.apache.hadoop.hive.metastore.HiveMetaException: Schema initialization FAILED! Metastore state would be inconsistent !!
Underlying cause: java.io.IOException : Schema script failed, errorcode 2
Use --verbose for detailed stacktrace.
*** schemaTool failed ***

# 你会发现有报错：这里你可以看，是hive-site.xml 默认的配置，你得配置没有生效，所以需要去删掉它的默认配置，这就是之前我们说要删除的原因
Metastore connection URL:	 jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :	 org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:	 APP

```
* （2） --verbose 可以加入这个参数 查看初始化的详细报错
```
[root@sun hive]# schematool -dbType mysql -initSchema --verbose
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/usr/local/hive/lib/log4j-slf4j-impl-2.6.2.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/usr/local/hadoop/share/hadoop/common/lib/slf4j-log4j12-1.7.10.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.apache.logging.slf4j.Log4jLoggerFactory]
Metastore connection URL:	 jdbc:mysql://192.168.2.31:3306/hive?createDatabaseIfNotExist
Metastore Connection Driver :	 org.mariadb.jdbc.Driver
Metastore connection User:	 root
org.apache.hadoop.hive.metastore.HiveMetaException: Failed to get schema version.
Underlying cause: java.sql.SQLException : Access denied for user 'root'@'sun.com' (using password: YES)
SQL Error code: 1045
org.apache.hadoop.hive.metastore.HiveMetaException: Failed to get schema version.
	at org.apache.hive.beeline.HiveSchemaHelper.getConnectionToMetastore(HiveSchemaHelper.java:77)
	at org.apache.hive.beeline.HiveSchemaTool.getConnectionToMetastore(HiveSchemaTool.java:144)
	at org.apache.hive.beeline.HiveSchemaTool.testConnectionToMetastore(HiveSchemaTool.java:473)
	at org.apache.hive.beeline.HiveSchemaTool.doInit(HiveSchemaTool.java:577)
	at org.apache.hive.beeline.HiveSchemaTool.doInit(HiveSchemaTool.java:563)
	at org.apache.hive.beeline.HiveSchemaTool.main(HiveSchemaTool.java:1145)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.apache.hadoop.util.RunJar.run(RunJar.java:226)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:141)
Caused by: java.sql.SQLException: Access denied for user 'root'@'sun.com' (using password: YES)
	at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1055)
	at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:956)
	at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:3491)
	at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:3423)
	at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:910)
	at com.mysql.jdbc.MysqlIO.secureAuth411(MysqlIO.java:3923)
	at com.mysql.jdbc.MysqlIO.doHandshake(MysqlIO.java:1273)
	at com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2031)
	at com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:718)
	at com.mysql.jdbc.JDBC4Connection.<init>(JDBC4Connection.java:46)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at com.mysql.jdbc.Util.handleNewInstance(Util.java:406)
	at com.mysql.jdbc.ConnectionImpl.getInstance(ConnectionImpl.java:302)
	at com.mysql.jdbc.NonRegisteringDriver.connect(NonRegisteringDriver.java:282)
	at java.sql.DriverManager.getConnection(DriverManager.java:664)
	at java.sql.DriverManager.getConnection(DriverManager.java:247)
	at org.apache.hive.beeline.HiveSchemaHelper.getConnectionToMetastore(HiveSchemaHelper.java:73)
	... 11 more
*** schemaTool failed ***
# 这个报错原因是，你之前安装mariadb的时候 授予root 权限没有成功，我遇到这个问题就是没有搞懂那个命令的参数的意思，这里再补充一下：

grant all privileges on *.* to root@”sun.com” identified by “123456”;   
#root登录mysql的用户名，sun.com你虚拟机的ip 123456 是你的mysql密码
```

```
# 成功！！！-----可能会有jar包冲突，可以不用管，看着不舒服可以删掉重复jar包，记得备份，以免出问题
beeline> 
beeline> Initialization script completed
schemaTool completed

hive> show tables;
OK
Time taken: 5.729 seconds
hive> 

```

* vi vim 编辑工具  快速定位行  vi 文件名 +n    eg: vi hive-site.xml +4338



