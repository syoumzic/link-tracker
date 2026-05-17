package tbank.academy.config

case class KafkaConfig(
    enabled: Boolean = true,
    bootstrapServers: List[String],
    topics: KafkaTopicsConfig,
    producer: KafkaProducerConfig,
    consumer: KafkaConsumerConfig
)

case class KafkaTopicsConfig(
    pullRequest: String = "link-updates-pull-request",
    issue: String = "link-updates-issue",
    comment: String = "link-updates-comment",
    answer: String = "link-updates-answer"
)

case class KafkaProducerConfig(
    acks: String = "1",
    retries: Int = 3,
    batchSize: Int = 16384
)

case class KafkaConsumerConfig(
    groupId: String = "bot-group",
    autoOffsetReset: String = "earliest",
    enableAutoCommit: Boolean = true,
    maxPollRecords: Int = 100,
    pollTimeoutMs: Long = 100
)
