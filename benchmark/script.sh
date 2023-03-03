#!/bin/sh
echo "Sending 2000 concurrent requests"
ab -q -T 'application/josn' -n 4000 -c 2000 -p postdata.dat http://localhost:8080/api/login
