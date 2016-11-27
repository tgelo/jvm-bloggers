package com.jvm_bloggers.core.data_fetching.blog_posts

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.JavaTestKit
import com.jvm_bloggers.core.data_fetching.blog_posts.domain.BlogPost
import com.jvm_bloggers.core.data_fetching.blog_posts.domain.BlogPostRepository
import com.jvm_bloggers.core.data_fetching.blogs.domain.Blog
import com.rometools.rome.feed.synd.SyndContent
import com.rometools.rome.feed.synd.SyndEntry
import scala.concurrent.duration.FiniteDuration
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

import static com.jvm_bloggers.utils.DateTimeUtilities.toLocalDateTime

class NewBlogPostStoringActorSpec extends Specification {

    BlogPostRepository blogPostRepository
    BlogPostFactory blogPostFactory
    JavaTestKit testProbe

    @Subject
    ActorRef blogPostingActor

    def setup() {
        ActorSystem system = ActorSystem.create("test")
        testProbe = new JavaTestKit(system)
        blogPostRepository = Mock(BlogPostRepository)
        blogPostFactory = Mock(BlogPostFactory)
        Props props = NewBlogPostStoringActor.props(blogPostRepository, blogPostFactory)
        blogPostingActor = system.actorOf(props, "blogPostingActor")
    }

    def cleanup() {
        testProbe.system.shutdown()
    }

    def "Should persist new blog post"() {
        given:
            String postUrl = "http://blogpost.com/blog"
            String postTitle = "Title"
            String postDescription = "description"
            Blog blog = Mock(Blog)
            BlogPost blogPost = Mock(BlogPost)
            SyndEntry entry = mockSyndEntry(postUrl, postTitle, postDescription)
            RssEntryWithAuthor message = new RssEntryWithAuthor(blog, entry)
            blogPostFactory.create(postTitle, postUrl, toLocalDateTime(entry.getPublishedDate()), blog) >> blogPost
            blogPostRepository.findByUrl(postUrl) >> Optional.empty()
        when:
            blogPostingActor.tell(message, ActorRef.noSender())
            testProbe.expectNoMsg(FiniteDuration.apply(1, "second"))
        then:
            1 * blogPostRepository.save(blogPost)
    }

    def "Should not persist blog post with invalid URL"() {
        given:
            String invalidLink = "invalidLink"
            String postTitle = "Title"
            String postDescription = "description"
            SyndEntry entry = mockSyndEntry(invalidLink, postTitle, postDescription)
            RssEntryWithAuthor message = new RssEntryWithAuthor(Mock(Blog), entry)
            blogPostRepository.findByUrl(invalidLink) >> Optional.empty()
        when:
            blogPostingActor.tell(message, ActorRef.noSender())
            testProbe.expectNoMsg(FiniteDuration.apply(1, "second"))
        then:
            0 * blogPostRepository.save({
                it.url == invalidLink &&
                        it.title == postTitle &&
                        it.description == postDescription
            })
    }

    def "Should update description if post already exists"() {
        given:
            String postUrl = "http://blogpost.com/blog"
            String postTitle = "Title"
            String postDescription = "description"
            SyndEntry entry = mockSyndEntry(postUrl, postTitle, postDescription)
            BlogPost blogPost = Mock()
            RssEntryWithAuthor message = new RssEntryWithAuthor(Mock(Blog), entry)
            blogPostRepository.findByUrl(postUrl) >> Optional.of(blogPost)
        when:
            blogPostingActor.tell(message, ActorRef.noSender())
            testProbe.expectNoMsg(FiniteDuration.apply(1, "second"))
        then:
            1 * blogPost.setDescription(postDescription)
        then:
            1 * blogPostRepository.save(blogPost)
    }

    def "Should use updatedDate if publishedDate is null"() {
        given:
            String postUrl = "http://blogpost.com/blog"
            String postTitle = "Title"
            Date updatedDate = new Date().minus(1)
            SyndEntry entry = mockSyndEntry(postUrl, postTitle, null, null, updatedDate)
            RssEntryWithAuthor message = new RssEntryWithAuthor(Mock(Blog), entry)
            blogPostRepository.findByUrl(postUrl) >> Optional.empty()
        when:
            blogPostingActor.tell(message, ActorRef.noSender())
            testProbe.expectNoMsg(FiniteDuration.apply(1, "second"))
        then:
            1 * blogPostFactory.create(postTitle, postUrl, toLocalDateTime(updatedDate), _ as Blog)
    }

    private SyndEntry mockSyndEntry(String postUrl, String postTitle, String postDescription) {
        return mockSyndEntry(postUrl, postTitle, postDescription, new Date(), new Date())
    }

    private SyndEntry mockSyndEntry(String postUrl, String postTitle, String postDescription, Date publishedDate, Date updatedDate) {
        SyndEntry entry = Mock(SyndEntry)
        entry.getPublishedDate() >> publishedDate
        entry.getUpdatedDate() >> updatedDate
        entry.getLink() >> postUrl
        entry.getTitle() >> postTitle
        entry.getDescription() >> Stub(SyndContent) {
            getValue() >> postDescription
        }
        return entry
    }

}
