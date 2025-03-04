#!/bin/bash

# Check if at least two parameters are passed
if [ $# -lt 2 ]; then
  echo "Usage: $0 <website> <number_of_packets>"
  exit 1
fi

# Assign parameters
WEBSITE=$1
PACKETS=$2

# Validate if the second parameter is a number
if ! [[ "$PACKETS" =~ ^[0-9]+$ ]]; then
  echo "Error: The second parameter must be a number."
  exit 1
fi

# Execute the ping command
ping -c "$PACKETS" "$WEBSITE"