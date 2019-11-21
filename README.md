# Clojurl

An example HTTP/S client CLI using Clojure and GraalVM native image.

Generated with [clj.native-cli template](https://github.com/taylorwood/clj.native-cli).
Uses deps.edn and [clj.native-image](https://github.com/taylorwood/clj.native-image).

## Prerequisites

- GraalVM 1.0.0-RC9 or higher
- Clojure

GraalVM 1.0.0-RC7 added HTTPS as a supported protocol, and this is a brief walkthrough
for using it in a Clojure project with GraalVM Community Edition for macOS.

Enable HTTPS protocol support with `native-image` options `--enable-https` or `--enable-url-protocols=https`.

#### Earlier versions of GraalVM

The following steps are only necessary with GraalVM 19.2.1 and earlier:

1. Configure path to `libsunec.dylib` on macOS (or `libsunec.do` on Linux)

   This shared object comes with the GraalVM distribution and can be found in
   `$GRAALVM_HOME/jre/lib/`. GraalVM uses `System.loadLibrary` to load it at run-time
   whenever it's first used. The file must either be in the current working directory,
   or in a path specified in Java system property `java.library.path`.

   I set the Java system property at run-time, before first HTTPS attempt:
   ```clojure
   (System/setProperty "java.library.path"
                       (str (System/getenv "GRAALVM_HOME") "/jre/lib"))
   ```

   See [this](https://github.com/oracle/graal/blob/master/substratevm/JCA-SECURITY-SERVICES.md#native-implementations)
   and [this](https://github.com/oracle/graal/blob/master/substratevm/URL-PROTOCOLS.md#https-support)
   for more information on HTTPS support in GraalVM and native images. If you're distributing
   a native image, you'll need to include libsunec. If it's in the same directory as your image
   you don't need to set `java.library.path`.

   You'll see a [warning](https://github.com/oracle/graal/blob/e3ef4f3f741d171a83c2dd2a0390dbede6b2c62d/substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/jdk/SecuritySubstitutions.java#L204)
   at run-time if this hasn't been properly configured:
   ```
   WARNING: The sunec native library could not be loaded.
   ```
1. Use more complete certificate store

   Some versions of GraalVM may have a smaller set of CA certificates. You can workaround this
   by replacing GraalVM's `cacerts`. I renamed the file and replaced it with a symbolic link
   to `cacerts` from the JRE that comes with macOS Mojave:
   ```bash
   $ mv $GRAALVM_HOME/jre/lib/security/cacerts $GRAALVM_HOME/jre/lib/security/cacerts.bak
   $ ln -s $(/usr/libexec/java_home)/jre/lib/security/cacerts $GRAALVM_HOME/jre/lib/security/cacerts
   ```

   If you don't do this, you might see errors like this when attempting HTTPS connections:
   ```
   Exception in thread "main" javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
   8<------------------------
   Caused by: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
   8<------------------------
   Caused by: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
   ```

## Usage

Compile the program with GraalVM `native-image`:
```bash
$ clojure -A:native-image
```

Print CLI options:
```
$ ./clojurl -h
  -u, --uri URI             URI of request
  -H, --header HEADER       Request header(s)
  -d, --data DATA           Request data
  -m, --method METHOD  GET  Request method e.g. GET, POST, etc.
  -o, --output FORMAT  edn  Output format e.g. edn, hickory
  -v, --verbose             Print verbose info
  -h, --help                Print this message
```
Responses can be printed in EDN or Hickory format.

Make a request and print response to stdout:
```
$ ./clojurl -u https://postman-echo.com/get
  {:headers
   {"content-encoding" "gzip",
    "content-type" "application/json; charset=utf-8",
    "date" "Fri, 05 Oct 2018 03:56:49 GMT",
    "etag" "W/\"10b-EZIoyNoyzUvEaPxY+kzMOEgaNh0\"",
    "server" "nginx",
    "vary" "Accept-Encoding",
    "content-length" "194",
    "connection" "keep-alive"},
   :status 200,
   :body
   "{\"args\":{},\"headers\":{\"host\":\"postman-echo.com\",\"accept\":\"text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\",\"accept-encoding\":\"gzip, deflate\",\"user-agent\":\"Java/1.8.0_172\",\"x-forwarded-port\":\"443\",\"x-forwarded-proto\":\"https\"},\"url\":\"https://postman-echo.com/get\"}"}
```
```
$ ./clojurl -H Accept=application/json -H X-Session-Id=1234 -H Content-Type=application/json \
     -u https://postman-echo.com/post \
     -m post -d "{'foo':true}"
  {:headers
   {"content-encoding" "gzip",
    "content-type" "application/json; charset=utf-8",
    "date" "Fri, 05 Oct 2018 03:57:06 GMT",
    "etag" "W/\"16d-FiL2opG823uS6YyXMHVrz5k+/Vk\"",
    "server" "nginx",
    "set-cookie"
    "sails.sid=s%3Af-U0lE-XKYPefMu_II_Sggg1HGVI4LlY.lbh1ZWAEX58lBuDVpo2vRZ%2FPAo1AHllJPSPsJ01RFvc; Path=/; HttpOnly",
    "vary" "Accept-Encoding",
    "content-length" "237",
    "connection" "keep-alive"},
   :status 200,
   :body
   "{\"args\":{},\"data\":\"{'foo':true}\",\"files\":{},\"form\":{},\"headers\":{\"host\":\"postman-echo.com\",\"content-length\":\"12\",\"accept\":\"application/json\",\"accept-encoding\":\"gzip, deflate\",\"content-type\":\"application/json\",\"user-agent\":\"Java/1.8.0_172\",\"x-session-id\":\"1234\",\"x-forwarded-port\":\"443\",\"x-forwarded-proto\":\"https\"},\"json\":null,\"url\":\"https://postman-echo.com/post\"}"}
```

As a proof-of-concept for using Clojure 1.9 + clojure.spec.alpha + Expound with GraalVM native-image,
the CLI options are validated using specs and invalid options can be explained using Expound:
```
$ ./clojurl -u https://postman-echo.com/get -o foo --verbose
Invalid option(s)
-- Spec failed --------------------

  {:headers ...,
   :method ...,
   :output-fn nil,
              ^^^
   :url ...,
   :verbose? ...}

should satisfy

  ifn?

-- Relevant specs -------

:clojurl/output-fn:
  clojure.core/ifn?
:clojurl/options:
  (clojure.spec.alpha/keys
   :req-un
   [:clojurl/url :clojurl/output-fn]
   :opt-un
   [:clojurl/method :clojurl/headers :clojurl/body])

-------------------------
Detected 1 error
```
