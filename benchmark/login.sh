#!/bin/sh
echo -e "\nSending 2000 concurrent requests\n\n"
ab -q -T 'application/json' -n 2000 -c 2000 -p postdata.dat http://localhost:8080/api/login
echo -e "\nSending 100000 request\n\n"
ab -q -T 'application/json' -n 100000 -c `nproc` -p postdata.dat http://localhost:8080/api/login
