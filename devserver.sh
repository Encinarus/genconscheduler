#!/bin/sh

# Do a clean every time? Yes. Because if A depends on B and you change
# B, the compiler is too stupid to realize that A needs to be compiled again,
# like if a method signature changed. gah.

# Install every time? Yes. Because the ear target isn't smart enough to recompile
# and reinstall the war if it's warrented. It doesn't seem to get from the target
# locally, you see. The install actually installs the war in a mvn directory on
# the local machine and the ear always looks there, instead of looking for the
# local artfact of the war. Without the install, the ear will use stale versions
# of the war. WAT!
mvn clean compile install && mvn -pl genconscheduler-ear appengine:devserver
