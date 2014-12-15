vert-x-transports
=================

Vert.x-Transports

Run gradle clean install to setup in local maven repository.


After upgrading to vertx 2.0, you now must set explicitly set the vertx instance on the VertxLocator. Basically
add VertxLocator.vertx = vertx somewhere in your code. See vramel for example.

Note: I didn't see tony's change here on 11/7. That change removed the runOnLoop call from VertxSender. I'd look closely
at that before using this.
