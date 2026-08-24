package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client;

import java.util.Objects;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.DescribeAlarmsOutput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleInput;
import software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetAlarmMuteRuleOutput;
import software.amazon.smithy.java.client.waiters.Waiter;
import software.amazon.smithy.java.client.waiters.backoff.BackoffStrategy;
import software.amazon.smithy.java.client.waiters.jmespath.Comparator;
import software.amazon.smithy.java.client.waiters.jmespath.JMESPathPredicate;
import software.amazon.smithy.java.client.waiters.matching.Matcher;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Waiters for the {@link CloudWatchClient} client.
 */
@SmithyGenerated
public record CloudWatchWaiter(CloudWatchClient client) {
    public CloudWatchWaiter {
        Objects.requireNonNull(client, "client cannot be null");
    }

    public Waiter<DescribeAlarmsInput, DescribeAlarmsOutput> alarmExists() {
        return Waiter.<DescribeAlarmsInput, DescribeAlarmsOutput>builder(client::describeAlarms)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.output(new JMESPathPredicate("length(MetricAlarms[]) > `0`", "true", Comparator.BOOLEAN_EQUALS)))
            .build();
    }

    public Waiter<DescribeAlarmsInput, DescribeAlarmsOutput> compositeAlarmExists() {
        return Waiter.<DescribeAlarmsInput, DescribeAlarmsOutput>builder(client::describeAlarms)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.output(new JMESPathPredicate("length(CompositeAlarms[]) > `0`", "true", Comparator.BOOLEAN_EQUALS)))
            .build();
    }

    public Waiter<DescribeAlarmsInput, DescribeAlarmsOutput> logAlarmExists() {
        return Waiter.<DescribeAlarmsInput, DescribeAlarmsOutput>builder(client::describeAlarms)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.output(new JMESPathPredicate("length(LogAlarms[]) > `0`", "true", Comparator.BOOLEAN_EQUALS)))
            .build();
    }

    public Waiter<GetAlarmMuteRuleInput, GetAlarmMuteRuleOutput> alarmMuteRuleExists() {
        return Waiter.<GetAlarmMuteRuleInput, GetAlarmMuteRuleOutput>builder(client::getAlarmMuteRule)
            .backoffStrategy(BackoffStrategy.getDefault(120000L, 5000L))
            .success(Matcher.success(true))
            .retry(Matcher.errorType("ResourceNotFoundException"))
            .build();
    }

}
