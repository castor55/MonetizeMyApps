package net.monetizemyapp

object Properties {
    //ProxyServerStarter server initialisation file.


    /**
     * Port on which the socks server should run
     * If not set deafults to 1080
     */
    const val port = 1080

    /**
     * Timeout settings for the ProxyServerStarter server,in milliseconds.
     * If not defined, all default to 3 minutes(18000ms).
     *
     * @see iddleTimeout  If no data received from/to user during this interval connection will be aborted.
     * @see acceptTimeout If no connection is accepted during this interval, connection will be aborted.
     * @see udpTimeout   If no datagrams are received from/to user, in this intervalUDP relay server stops,
     * and control connection is closed.
     * Any of these can be 0, implying infinit timeout, that is once the
     * connection is made, it is kept alive until one of the parties closes it.
     * In case of the BIND command, it implies that server will be listenning
     * for incoming connection until it is accepted, or until client closes
     * control connection.
     * For UDP servers it implies, that they will run as long, as client
     * keeps control connection open.
     */

    const val iddleTimeout = 600000   // 10 minutes
    const val acceptTimeout = 60000    // 1 minute
    const val udpTimeout = 600000   // 10 minutes

    /**
     * @see datagramSize -- Size of the datagrams to use for udp relaying.
     * Defaults to 64K bytes(0xFFFF = 65535 a bit more than maximum possible size).
     */
    const val datagramSize = 8192


    /**
     * @see log -- Name of the file, to which logging should be done
     * If log is - (minus sine) do logging to standart output.
     * Optional field, if not defined, no logging is done.
     */
    const val log = "-"

    /**
     * @see host -- Host on which to run, for multihomed hosts,
     * Default -- all(system dependent)
     */

    const val host = "localhost"


    /**
     * @see range -- Semicolon(;) separated range of addresses, from which
     * connections should be accepted.
     *
     *      Range could be one of those
     *          1. Stand alone host name -- some.host.com or 33.33.44.101
     *          2. Host range
     *    .my.domain.net / 190.220.34.
     *    host1:host2 / 33.44.100:33.44.200
     *
     *  Example: .myDomain.com;100.220.30.;myHome.host.com;\
     *  comp1.myWork.com:comp10.myWork.com
     *
     *  This will include all computers in the domain myDomain.com,
     *  all computers whose addresses start with 100.200.30,
     *  host with the name myHome.host.com,
     *  and computers comp1 through to comp2 in the domain myWork.com,
     *  assuming their names correspond to there ip addresses.
     *
     *    NOTE: Dot(.) by itself implies all hosts, be sure not to include
     *    one of those.
     */

    const val range = "localhost"


    /**
     * @see users
     * Semicolon(;) separated list of users, for whom permissions should be
     * granted, given they connect from one of the hosts speciefied by range.
     * This field is optional, if not defined, ANY user will be allowed to use
     * ProxyServerStarter server, given he\she is connecting from one of the hosts in the
     * range.
     * NOTE:  Whitespaces are not ignored (except for the first name, it's how java
     * parses Property files).
     * User names are CASESensitive.
     * You have been warned.
     * NOTE2: Misspelling users with Users, or anything, will be understood as
     * if users were not defined, and hence will imply that ANYBODY, will
     * be granted access from the hosts in the range.
     */

    const val users = "root"

    /**
     * Proxy configurations, that is what proxy this proxy should use, if any.
     * @see proxy should be a semicolon(;) separated list of proxy entries.
     * Each entry should be in the form: host[:port:user:password].
     * If only host is supplied SOCKSv5 proxy is assumed, running on port 1080.
     * If user is supplied,  but password not supplied, SOCKSv4 is assumed,
     * running oon machine 'host' on port 'port', user name will be supplied as
     * authentication for that proxy.
     * If both user and password is supplied, SOCKSv5 proxy is assumed, with
     * user/password authentication.
     *
     * directHosts should contain ;-separated list of inetaddress and ranges.
     * These machines will be addressed directly rather than through
     * the proxy. See range for more details, what sort of entries
     * permitted and understood.*/


    const val proxy = "127.0.0.1;"
    const val directHosts = "localhost"
}