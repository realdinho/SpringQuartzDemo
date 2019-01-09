# SpringQuartzDemo
A simple demo API to schedule tasks with Spring Boot and Quartz frameworks

API name: http://localhost:8080/schedule/email

Body format: application/json

Body:
{
	"email" : "dias.realdo@isentric.com",
	"subject" : "Test Email from Spring Boot Cron Job",
	"body" : "Dear Realdo, <br><br> Congratulations! You're now using Spring boot with Cron Job. <br> Regards.",
	"dateTime": "2019-01-08T16:05:00",
	"timeZone" : "Asia/Kuala_Lumpur"
}
