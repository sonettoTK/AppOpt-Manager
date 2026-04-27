# AppOpt Manager

一个基于 Material Design 3 的 Android 应用配置管理工具，用于管理自定义线程CPU亲和性Magisk模块——AppOpt的配置文件。

> 声明：本工具仅用于管理AppOpt的配置文件，不涉及任何应用数据的读写操作，不涉及任何系统级的权限管理，不提供CPU核心绑定的功能。

## 技术栈

Jetpack Compose + Material Design 3 | MVVM + Repository | Kotlin Coroutines + Flow

## 配置文件格式

```
# 应用名称
包名=CPU核心列表
包名:子进程名=CPU核心列表
包名:子进程名{线程名}=CPU核心列表
```

示例：
```
# 微信
com.tencent.mm=0-3
com.tencent.mm:push=0-1
com.tencent.mm:push{Thread-1}=0
```


