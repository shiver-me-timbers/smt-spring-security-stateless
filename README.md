<!---
Copyright 2015 Karl Bennett

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
smt-spring-security-stateless
===========
[![Build Status](https://travis-ci.org/shiver-me-timbers/smt-spring-security-stateless.svg?branch=master)](https://travis-ci.org/shiver-me-timbers/smt-spring-security-stateless) [![Coverage Status](https://coveralls.io/repos/shiver-me-timbers/smt-spring-security-stateless/badge.svg?branch=master&service=github)](https://coveralls.io/github/shiver-me-timbers/smt-spring-security-stateless?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.shiver-me-timbers/smt-spring-security-stateless/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.shiver-me-timbers/smt-spring-security-stateless/)

This library contains two customisable classes that help with configuring Spring Security to be stateless. This library
does not provide any help with the initial sign in, it will just take over after the sign in has succeed and for all
subsequent authentication checks after that.

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.github.shiver-me-timbers</groupId>
        <artifactId>smt-spring-security-stateless</artifactId>
        <version>1.2</version>
    </dependency>
</dependencies>
```

### Usage

#### [StatelessAuthenticationSuccessHandler](src/main/java/shiver/me/timbers/security/spring/StatelessAuthenticationSuccessHandler.java)

This handler will be called after a successful authentication/sign, it adds an `X-AUTH-TOKEN` header and cookie to the
response that contains a token (default is JWT). This token or cookie should then be supplied with every subsequent
request.

This handler can be added to Spring Security with the
[`HttpSecurity`](http://docs.spring.io/spring-security/site/docs/4.0.2.RELEASE/reference/htmlsingle/#jc-httpsecurity)
fluent API.

```java
http.formLogin().successHandler(
    new StatelessAuthenticationSuccessHandler("some secret", "/app/root")
);
```

This class can be further configured to through it's constructors, one common customisation would be to add a more
secure [`TokenFactory`](src/main/java/shiver/me/timbers/security/token/TokenFactory.java) the default factory doesn't
have any expiration date.

```java
http.formLogin().successHandler(
    new StatelessAuthenticationSuccessHandler(new ExpringTokenFactory(), "/app/root")
);
```

#### [StatelessAuthenticationFilter](src/main/java/shiver/me/timbers/security/spring/StatelessAuthenticationFilter.java)

This filter will check every request for the `X-AUTH-TOKEN` header or cookie and if it is valid authorise the request. It
must be configured with the same secret or `TokenFactory` that was used in the `StatelessAuthenticationSuccessHandler`.

The filter can also be added to Spring Security with the `HttpSecurity`. It must be added before the Spring Security
internal [`UsernamePasswordAuthenticationFilter`](http://docs.spring.io/spring-security/site/docs/4.0.2.RELEASE/reference/htmlsingle/#ns-custom-filters)
so that it will authorise the request before a sign in is required.

```java
http.addFilterBefore(
    new StatelessAuthenticationFilter("some secret"),
    UsernamePasswordAuthenticationFilter.class
);
```

Further customisation can also be done using it's constructors. Again, supplying a better `TokenFactory` would be a good
example.

```java
http.addFilterBefore(
    new StatelessAuthenticationFilter(new ExpringTokenFactory()),
    UsernamePasswordAuthenticationFilter.class
);
```

#### Default Behaviour

When using the simple string only constructors the handler and filter will do default token and authentication
generation. The default generation is very simple, but could be enough in some situations.

##### Default Token

The default generated `X-AUTH-TOKEN` is a [JWT](http://jwt.io/) token that is generated from the Spring Security
authentication name with no expiry date. That is it will be valid for ever and can only be invalidated by changing the
global application secret. This isn't very secure, so in most situations you will want to override it by creating your
own `TokenFactory` implementation as mentioned above. The
[`BasicJwtTokenFactory`](src/main/java/shiver/me/timbers/security/token/BasicJwtTokenFactory.java) is a good example to
help you get started on a more secure JWT `TokenFactory`. Though you are of course free to use any token generation
strategy of your choosing.

##### Default Authentication

An [`AuthenticatedAuthentication`](src/main/java/shiver/me/timbers/security/spring/AuthenticatedAuthentication.java) is
generated by default. This should actually be enough in most simple situations. If you are not going to need to
lock/disable accounts or use roles then you will not need to customise this behaviour.

Though, if you do need to customise it, say to add the ability to lock accounts then you can provide your own
[`AuthenticationFactory`](src/main/java/shiver/me/timbers/security/spring/AuthenticationFactory.java).

```java
http.addFilterBefore(
    new StatelessAuthenticationFilter(tokenFactory, new LockableAuthenticationFactory()),
    UsernamePasswordAuthenticationFilter.class
);
```

### Examples

A couple examples have been provide to show how to use this library.

#### [smt-spring-security-stateless-basic](https://github.com/shiver-me-timbers/smt-spring-security-stateless-examples/tree/master/smt-spring-security-stateless-basic)

This example shows how to use the library with it's default configuration.

#### [smt-spring-security-stateless-advanced](https://github.com/shiver-me-timbers/smt-spring-security-stateless-examples/tree/master/smt-spring-security-stateless-advanced)

This example shows how you can customise the library to generate your own tokens and authentications.