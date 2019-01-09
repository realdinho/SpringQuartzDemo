package com.isentric.quartz.controller;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.isentric.quartz.job.EmailJob;
import com.isentric.quartz.model.ScheduleEmailRequest;
import com.isentric.quartz.model.ScheduleEmailResponse;

@RestController
public class EmailJobSchedulerController
{
	private static final Logger logger = LoggerFactory.getLogger(EmailJobSchedulerController.class);
	
	@Autowired
	private Scheduler scheduler;
	
	@PostMapping("/schedule/email")
	public ResponseEntity<ScheduleEmailResponse> scheduleEmail(@Valid @RequestBody ScheduleEmailRequest scheduleEmailRequest)
	{
		ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse();
		
		try 
		{
			ZonedDateTime dateTime = ZonedDateTime.of(scheduleEmailRequest.getDateTime(), scheduleEmailRequest.getTimeZone());
			
			if(dateTime.isBefore(ZonedDateTime.now()))
			{
				scheduleEmailResponse = new ScheduleEmailResponse(false, "DateTime must be after current time");
				
				return ResponseEntity.badRequest().body(scheduleEmailResponse);
			}
			
			JobDetail jobDetail = buildJobDetail(scheduleEmailRequest);
			Trigger trigger = buildJobTrigger(jobDetail, dateTime);
			scheduler.scheduleJob(jobDetail, trigger);
			
			scheduleEmailResponse = new ScheduleEmailResponse(true, jobDetail.getKey().getName(), 
					jobDetail.getKey().getGroup(), "Email scheduled successfully!");
			
			return ResponseEntity.ok(scheduleEmailResponse);
		} 
		catch (SchedulerException ex) 
		{
			logger.error("Error scheduling email", ex);
			
			scheduleEmailResponse = new ScheduleEmailResponse(false, "Error scheduling email. Please try later!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(scheduleEmailResponse);
		}
	}
	
	private JobDetail buildJobDetail(@Valid ScheduleEmailRequest scheduleEmailReq)
	{
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("email", scheduleEmailReq.getEmail());
		jobDataMap.put("subject", scheduleEmailReq.getSubject());
		jobDataMap.put("body", scheduleEmailReq.getBody());
		
		return JobBuilder.newJob(EmailJob.class)
				.withIdentity(UUID.randomUUID().toString(), "email-jobs")
				.withDescription("Send Email Job")
				.usingJobData(jobDataMap)
				.storeDurably()
				.build();
	}

	private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt)
	{
		return TriggerBuilder.newTrigger()
				.forJob(jobDetail)
				.withIdentity(jobDetail.getKey().getName(), "email-triggers")
				.withDescription("Send Email Trigger")
				.startAt(Date.from(startAt.toInstant()))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
				.build();
	}
	
	private Trigger sendEmailReminder(JobDetail job)
	{
		return TriggerBuilder.newTrigger().forJob(job)
				.withIdentity(job.getKey().getName(), "real_qrtz_trigger")
				.withDescription("Simple Trigger by Realdo")
				.withSchedule(SimpleScheduleBuilder.repeatMinutelyForever().withIntervalInMinutes(1))
				.build();
	}
	
}
