#!/bin/bash

# Kafka Topic Setup Script for Link Tracker
# This script creates the necessary Kafka topics for notification delivery

set -e

KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"
PARTITIONS="${PARTITIONS:-3}"
REPLICATION_FACTOR="${REPLICATION_FACTOR:-3}"

TOPICS=(
  "link-updates-pull-request"
  "link-updates-issue"
  "link-updates-comment"
  "link-updates-answer"
)

echo "Creating Kafka topics..."
echo "Broker: $KAFKA_BROKER"
echo "Partitions: $PARTITIONS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo ""

for TOPIC_NAME in "${TOPICS[@]}"; do
  echo "Creating topic: $TOPIC_NAME"
  
  docker run --rm --network link-tracker_kafka-network \
    apache/kafka:4.0.0 \
    /opt/kafka/bin/kafka-topics.sh \
    --create \
    --topic "$TOPIC_NAME" \
    --bootstrap-server "$KAFKA_BROKER" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION_FACTOR" \
    --config retention.ms=604800000 \
    --config cleanup.policy=delete \
    --if-not-exists
  
  echo "Topic '$TOPIC_NAME' created successfully!"
  echo ""
done

echo "All topics created!"
echo ""
echo "Topic configuration rationale:"
echo "- Partitions: $PARTITIONS - Allows parallel consumption by multiple bot instances"
echo "- Replication Factor: $REPLICATION_FACTOR - Ensures fault tolerance with 3 brokers"
echo "- Retention: 7 days (604800000ms) - Sufficient time for consumers to process messages"
echo "- Cleanup Policy: delete - Old messages are removed after retention period"
echo ""
echo "Separate topics for each message type simplify:"
echo "- Serialization/deserialization (no type discrimination needed)"
echo "- Independent scaling per message type"
echo "- Easier debugging and monitoring"
echo "- Selective consumption (can consume only specific message types)"
