#!/bin/bash
echo "******CREATING DOCKER DATABASE******"
gosu postgres postgres --single <<- EOSQL
   CREATE DATABASE tellme;
   CREATE USER tellme WITH PASSWORD 'letmein';
   GRANT ALL PRIVILEGES ON DATABASE tellme to tellme;
EOSQL
echo ""
echo "******DOCKER DATABASE CREATED******"