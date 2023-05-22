# weekend-dns

(Native Executable) Command Line DNS Resolver

A Clojure implementation of Julia Evans' wonderful toy DNS resolver
[https://implement-dns.wizardzines.com/](https://implement-dns.wizardzines.com/)

The motivation was to see how low level binary network protocols could be
written with Clojure and Java interop, see how DNS protocols work in detail, and
implement my own DNS resolver. I additionally used GraalVM to build it into a
small, fast native image.

## DNS Specifications 

[RFC 1034 Domain Names - Concepts and Facilities](https://datatracker.ietf.org/doc/html/rfc1034)

[RFC 1035 Domain Names - Implementation and Specification](https://datatracker.ietf.org/doc/html/rfc1035)

## Requirements 

- Java 17+ (most should work with Java 8 except testing sample inputs/outputs
  with `hexify/unhexify` which rely on Java's `HexFormat`)

- [Clojure](https://clojure.org/guides/install_clojure) 

  *Optional to build native executable*
  
- [GraalVM](https://github.com/graalvm/graalvm-ce-builds/releases) 

  Unpack the package in your installation folder and add it to the path 

``` 
    $ export GRAALVM_HOME=/full/path/to/graalvm
    $ export PATH=$GRAALVM_HOME/bin:$PATH 
    $ gu install native-image
```

## Installation

```
    $ git clone https://github.com/iwrotesomecode/weekend-dns.git
```


If creating a native executable with GraalVM, enter the directory and additionally run the build script 

```
    $ ./build.sh
```


## Usage

To run the project with Clojure, provide arguments for a domain, e.g. "www.example.com", and record type, e.g. TYPE-A or 1:

    $ clj -M:run www.example.com TYPE-A 
    
    "Querying 198.41.0.4 for www.example.com"
    "Querying 192.12.94.30 for www.example.com"
    "Querying 198.41.0.4 for a.iana-servers.net"
    "Querying 192.12.94.30 for a.iana-servers.net"
    "Querying 199.43.135.53 for a.iana-servers.net"
    "NS-domain a.iana-servers.net found at 199.43.135.53"
    "Querying 199.43.135.53 for www.example.com"
    "93.184.216.34"

To run the project from the native executable after building it with GraalVM, run:
    
    $ ./dns www.uchicago.edu 1

    "Querying 198.41.0.4 for www.uchicago.edu"
    "Querying 192.5.6.30 for www.uchicago.edu"
    "Querying 128.135.249.250 for www.uchicago.edu"
    "Querying 198.41.0.4 for mc-1b49d921-43a2-4264-88fd-edb899e7492a-afd.azurefd.net"
    "Querying 192.5.6.30 for mc-1b49d921-43a2-4264-88fd-edb899e7492a-afd.azurefd.net"
    "Querying 150.171.16.37 for mc-1b49d921-43a2-4264-88fd-edb899e7492a-afd.azurefd.net"
    "Querying 198.41.0.4 for star-azurefd-prod.trafficmanager.net"
    "Querying 192.5.6.30 for star-azurefd-prod.trafficmanager.net"
    "Querying 198.41.0.4 for tm1.edgedns-tm.info"
    "Querying 199.254.31.1 for tm1.edgedns-tm.info"
    "Querying 208.84.5.4 for tm1.edgedns-tm.info"
    "NS-domain tm1.edgedns-tm.info found at 13.107.222.240"
    "Querying 13.107.222.240 for star-azurefd-prod.trafficmanager.net"
    "Querying 198.41.0.4 for shed.dual-low.part-0043.t-0009.fdv2-t-msedge.net"
    "Querying 192.5.6.30 for shed.dual-low.part-0043.t-0009.fdv2-t-msedge.net"
    "Querying 13.107.237.1 for shed.dual-low.part-0043.t-0009.fdv2-t-msedge.net"
    "CNAME shed.dual-low.part-0043.t-0009.fdv2-t-msedge.net resolved"
    "CNAME star-azurefd-prod.trafficmanager.net resolved"
    "CNAME mc-1b49d921-43a2-4264-88fd-edb899e7492a-afd.azurefd.net resolved"
    "13.107.237.71"

    
To monitor traffic, you can additionally run:

    $ sudo tcpdump -ni any port 53

    20:10:43.792269 wlo1  Out IP 192.168.1.13.51934 > 198.41.0.4.53: 13099 A? example.com. (29)
    20:10:43.809389 wlo1  In  IP 198.41.0.4.53 > 192.168.1.13.51934: 13099- 0/13/14 (489)
    20:10:43.811043 wlo1  Out IP 192.168.1.13.51934 > 192.5.6.30.53: 46963 A? example.com. (29)
    20:10:43.839439 wlo1  In  IP 192.5.6.30.53 > 192.168.1.13.51934: 46963- 0/2/0 (77)
    20:10:43.840559 wlo1  Out IP 192.168.1.13.51934 > 198.41.0.4.53: 48004 A? a.iana-servers.net. (36)
    20:10:43.856145 wlo1  In  IP 198.41.0.4.53 > 192.168.1.13.51934: 48004- 0/13/14 (493)
    20:10:43.859153 wlo1  Out IP 192.168.1.13.51934 > 192.5.6.30.53: 29805 A? a.iana-servers.net. (36)
    20:10:43.885239 wlo1  In  IP 192.5.6.30.53 > 192.168.1.13.51934: 29805- 0/4/6 (240)
    20:10:43.886803 wlo1  Out IP 192.168.1.13.51934 > 199.43.135.53.53: 62564 A? a.iana-servers.net. (36)
    20:10:43.901722 wlo1  In  IP 199.43.135.53.53 > 192.168.1.13.51934: 62564*- 1/0/0 A 199.43.135.53 (52)
    20:10:43.902754 wlo1  Out IP 192.168.1.13.51934 > 199.43.135.53.53: 3738 A? example.com. (29)
    20:10:43.919264 wlo1  In  IP 199.43.135.53.53 > 192.168.1.13.51934: 3738*- 1/0/0 A 93.184.216.34 (45)

## Limitations 

- This resolver is only able to query **A** records and additionally parse types **NS**, and **CNAME**. 

-  ~~There is a possible exploit in the DNS compression that would lead to an infinite loop if a malicious actor sent a DNS response with a compression entry that points to itself.~~
*Added check for maximum compression pointers (126). [rationale](https://github.com/miekg/dns/blob/b3dfea07155dbe4baafd90792c67b85a3bf5be23/msg.go#L24-L36)*

- Caching not implemented. 

- EDNS0 (extended DNS) not implemented.

## Implementation Notes 

One particular hiccup in following along is the shortcoming of Java types. There
are no unsigned types except char, requiring a little mental accounting when
working with byte arrays and comparing with unsigned example outputs. In this
context, it meant using Clojure's `unchecked-short` to coerce 2-byte numbers,
and recognizing that "32,768" is presented in two's complement as "-32,768" and
"65,535" is presented as "-1", though they have the same bit representations.
When relying on the actual representational value (e.g. to pass as an argument
where automatic type promotion happens otherwise), cast to an unsigned int:

```
;; equivalent cast methods

(Short/toUnsignedInt -1) ;; => 65535
(bit-and -1 0xffff)      ;; => 65535

;; to see binary representation

(Integer/toBinaryString (bit-and 0xffff -32768)) ;; => "1000000000000000"
(Integer/toBinaryString 32768)                   ;; => "1000000000000000"

```

When using some of the stream methods, like `.readByte`, the returned value in
Clojure gets promoted to `Long`, and may need to be recast, for instance when
packing individual bytes to [create the compression
pointer](https://github.com/iwrotesomecode/weekend-dns/blob/ded771ac24e94716f7802d6f00b849b848137760/src/weekend_dns/part2.clj#L57C1-L64).

I wrapped the network response in Java's `DataInputStream` to make it easier to
handle reading bytes. Unfortunately none of Java's default Input Streams allow
random access or `seek`, only the ability to `mark/reset` and `skip`. Seeking is
necessary to parse DNS compression, however. I didn't want to write it to a file
to leverage `seek`from Java's File io libraries, so I created a new stream at
the pointer offset anytime I encountered a compression pointer. 

I followed [Kira McLean's
guide](https://dev.to/kiraemclean/building-a-fast-command-line-app-with-clojure-1kc8)
to build a native executable with GraalVM, with some changes to prefer
[clojure/tools.build](https://github.com/clojure/tools.build). In order to
properly compile the executable it was necessary to fix reflection warnings,
adding `(set! *warn-on-reflection* true)` in all my source files, and also to
use `delay` to instantiate the `DatagramSocket` at runtime.
