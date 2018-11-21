#!/bin/bash

echo "Bundle install"
bundle install

echo "Starting application"
ruby -v ./application.rb
