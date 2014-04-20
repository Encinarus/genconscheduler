#!/bin/sh

mvn clean compile install && mvn -pl genconscheduler-ear appengine:devserver
