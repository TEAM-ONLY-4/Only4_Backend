package com.ureca.only4_be.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventBridgeSchedulerService {

    @Value("${cloud.aws.ecs.cluster-name:}")
    private String clusterName;

    @Value("${cloud.aws.ecs.region:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.ecs.subnet-ids:}")
    private String subnetIds;

    @Value("${cloud.aws.ecs.security-group-ids:}")
    private String securityGroupIds;

    @Value("${cloud.aws.ecs.task-definitions.notification:only4-notification-task}")
    private String notificationTaskDefinitionArn;

    // Scheduler가 ECS Task를 실행할 때 사용할 Role ARN (EventBridge Scheduler용 Role)
    // 이 Role은 ecs:RunTask 권한과 iam:PassRole 권한을 가지고 있어야 함
    @Value("${cloud.aws.scheduler.execution-role-arn:}")
    private String schedulerExecutionRoleArn;

    private static final String SCHEDULE_GROUP_NAME = "default"; 

    /**
     * EventBridge Scheduler에 1회성 스케줄 생성
     */
    public void createSchedule(Long reservationId, LocalDateTime executeAt) {
        if (schedulerExecutionRoleArn == null || schedulerExecutionRoleArn.isEmpty()) {
            log.warn("Scheduler Execution Role ARN is missing. Skipping schedule creation.");
            return;
        }

        try (SchedulerClient client = SchedulerClient.builder().region(Region.of(region)).build()) {

            String scheduleName = "reservation-" + reservationId;
            // EventBridge Scheduler는 UTC 기준 ISO8601 형식을 권장하지만, at()으로 로컬 타임존 지정 가능
            // 예: "2026-01-25T10:00:00"
            String atExpression = "at(" + executeAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ")";

            // ECS Parameters 설정
            EcsParameters ecsParams = EcsParameters.builder()
                    .taskDefinitionArn(notificationTaskDefinitionArn)
                    .launchType(LaunchType.FARGATE)
                    .networkConfiguration(NetworkConfiguration.builder()
                            .awsvpcConfiguration(AwsVpcConfiguration.builder()
                                    .subnets(subnetIds.split(","))
                                    .securityGroups(securityGroupIds.split(","))
                                    .assignPublicIp(AssignPublicIp.ENABLED)
                                    .build())
                            .build())
                    .build();

            // Target 설정 (ECS RunTask)
            Target target = Target.builder()
                    .arn("arn:aws:ecs:" + region + ":" + getAccountId() + ":cluster/" + clusterName) // Cluster ARN
                    .roleArn(schedulerExecutionRoleArn) // Scheduler가 사용할 Role
                    .ecsParameters(ecsParams)
                    .input("{\"containerOverrides\": [{\"name\": \"notification-container\", \"environment\": [{\"name\": \"run.id\", \"value\": \"" + System.currentTimeMillis() + "\"}]}]}")
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(SCHEDULE_GROUP_NAME)
                    .scheduleExpression(atExpression)
                    .scheduleExpressionTimezone("Asia/Seoul") // 한국 시간 기준
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .target(target)
                    .description("Only4 Reservation Notification for ID: " + reservationId) // 실행 후 스케줄 자동 삭제
                    .actionAfterCompletion(ActionAfterCompletion.DELETE) 
                    .build();

            client.createSchedule(request);
            log.info("Created EventBridge Schedule: {} at {}", scheduleName, executeAt);

        } catch (Exception e) {
            log.error("Failed to create EventBridge schedule", e);
            throw new RuntimeException("스케줄 생성 실패", e);
        }
    }

    /**
     * 스케줄 삭제 (예약 취소 시)
     */
    public void deleteSchedule(Long reservationId) {
        try (SchedulerClient client = SchedulerClient.builder().region(Region.of(region)).build()) {
            String scheduleName = "reservation-" + reservationId;
            
            DeleteScheduleRequest request = DeleteScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(SCHEDULE_GROUP_NAME)
                    .build();

            client.deleteSchedule(request);
            log.info("Deleted EventBridge Schedule: {}", scheduleName);
        } catch (ResourceNotFoundException e) {
            log.warn("Schedule not found, skipping delete: reservation-{}", reservationId);
        } catch (Exception e) {
            log.error("Failed to delete EventBridge schedule", e);
            throw new RuntimeException("스케줄 삭제 실패", e);
        }
    }
    
    // 편의상 Account ID를 가져오거나 설정으로 받아야 함. 
    // 여기서는 ARN 파싱 대신 간단히 환경변수 주입을 권장하지만, 임시로 로직 내에서 처리하려면 STS 호출이 필요.
    // 일단 .env나 yml에서 주입받는 구조로 가정하고 Getter 추가.
    @Value("${cloud.aws.account-id:}")
    private String accountId;
    
    private String getAccountId() {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalStateException("AWS Account ID is not configured.");
        }
        return accountId;
    }
}
