package com.jvm_bloggers.core.mailing.sender

import com.jvm_bloggers.TestNowProvider
import com.jvm_bloggers.entities.email.Email
import com.jvm_bloggers.entities.email.EmailRepository
import com.jvm_bloggers.utils.NowProvider
import javaslang.control.Option
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class EmailSendingSchedulerSpec extends Specification {

    LocalDateTime NOW = LocalDateTime.now();
    EmailRepository emailRepository = Mock(EmailRepository)
    MailSender mailSender = Mock(MailSender)
    NowProvider nowProvider = new TestNowProvider(NOW)

    @Subject
    EmailSendingScheduler emailSendingScheduler = new EmailSendingScheduler(emailRepository, mailSender, nowProvider)

    def "Should save sent email with set sentDate"() {
        given:
        Email email = Mock(Email)
        emailRepository.findFirstBySentDateNull() >> Option.of(email)
        mailSender.sendEmail(_, _, _, _) >> MailSender.EmailSendingStatus.SUCCESS

        when:
        emailSendingScheduler.sendOneEmail()

        then:
        1 * email.setSentDate(NOW)
        1 * emailRepository.save(email)
    }

    def "Should not execute any action for zero not sent emails"() {
        given:
        emailRepository.findFirstBySentDateNull() >> Option.none()

        when:
        emailSendingScheduler.sendOneEmail()

        then:
        0 * mailSender.sendEmail(_, _, _, _)
        0 * emailRepository.save(_ as Email)
    }

    def "Should not update sentDate for unsuccessful sanding action"() {
        given:
        Email email = Mock(Email)
        emailRepository.findFirstBySentDateNull() >> Option.of(email)
        mailSender.sendEmail(_, _, _, _) >> MailSender.EmailSendingStatus.ERROR

        when:
        emailSendingScheduler.sendOneEmail()

        then:
        0 * email.setSentDate(NOW)
        0 * emailRepository.save(email)
    }

}
