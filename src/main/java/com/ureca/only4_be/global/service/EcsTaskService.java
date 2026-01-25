package com.ureca.only4_be.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcsTaskService {

    @Value("${cloud.aws.ecs.cluster-name:}")
    private String clusterName;

    @Value("${cloud.aws.ecs.region:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.ecs.subnet-ids:}")
    private String subnetIds;

    @Value("${cloud.aws.ecs.security-group-ids:}")
    private String securityGroupIds;

    public String runTask(String taskDefinitionName, String containerName, List<KeyValuePair> envVars) {
        if (clusterName == null || clusterName.isEmpty()) {
            log.warn("ECS cluster name is not configured. Skipping ECS task execution.");
            return "ECS_NOT_CONFIGURED";
        }

        try (EcsClient ecsClient = EcsClient.builder().region(Region.of(region)).build()) {
            AwsVpcConfiguration vpcConfig = AwsVpcConfiguration.builder()
                    .subnets(subnetIds.split(","))
                    .securityGroups(securityGroupIds.split(","))
                    .assignPublicIp(AssignPublicIp.ENABLED)
                    .build();

            NetworkConfiguration networkConfig = NetworkConfiguration.builder()
                    .awsvpcConfiguration(vpcConfig)
                    .build();

            List<String> command = envVars.stream()
                    .map(kv -> kv.name() + "=" + kv.value())
                    .toList();

            ContainerOverride containerOverride = ContainerOverride.builder()
                    .name(containerName)
                    .command(command)
                    .build();

            TaskOverride taskOverride = TaskOverride.builder()
                    .containerOverrides(containerOverride)
                    .build();

            RunTaskRequest runTaskRequest = RunTaskRequest.builder()
                    .cluster(clusterName)
                    .taskDefinition(taskDefinitionName)
                    .launchType(LaunchType.FARGATE)
                    .networkConfiguration(networkConfig)
                    .overrides(taskOverride)
                    .count(1)
                    .build();

            RunTaskResponse response = ecsClient.runTask(runTaskRequest);
            String taskArn = response.tasks().getFirst().taskArn();
            log.info("ECS Task Started: {}", taskArn);
            return taskArn;

        } catch (Exception e) {
            log.error("Failed to run ECS task", e);
            throw new RuntimeException("ECS Task 실행 실패: " + e.getMessage(), e);
        }
    }
}
