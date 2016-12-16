package com.jvm_bloggers.frontend.all_issues;

import com.jvm_bloggers.frontend.AbstractFrontendPage;
import com.jvm_bloggers.frontend.all_issues.all_issues_panel.AllIssuesPanel;
import com.jvm_bloggers.frontend.newsletter_issue.NewsletterIssueDto;
import com.jvm_bloggers.frontend.newsletter_issue.NewsletterIssueDtoService;
import com.jvm_bloggers.frontend.newsletter_issue.NewsletterIssuePage;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.jvm_bloggers.utils.DateTimeUtilities.DATE_FORMATTER;
import static com.jvm_bloggers.utils.DateTimeUtilities.getPolishMonthAndYear;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@MountPath("all-issues")
public class AllIssuesPage extends AbstractFrontendPage {

    private static final String ALL_ISSUES_PANEL_ID = "allIssuesPanel";

    @SpringBean
    private NewsletterIssueDtoService newsletterIssueDtoService;

    public AllIssuesPage() {
        SortedMap<String, List<Link<?>>> allIssuesGroups = createAllMonthGroups(
            newsletterIssueDtoService);
        add(new AllIssuesPanel(ALL_ISSUES_PANEL_ID, allIssuesGroups));
    }

    private SortedMap<String, List<Link<?>>> createAllMonthGroups(
        NewsletterIssueDtoService newsletterIssueDtoService) {
        return newsletterIssueDtoService
            .findAllByOrderByPublishedDateDesc().stream()
            .collect(groupingBy(
                this::getIssuesGroupName,
                TreeMap::new,
                mapping(this::getLink, toList())));
    }

    private String getIssuesGroupName(NewsletterIssueDto issue) {
        return getPolishMonthAndYear(issue.publishedDate);
    }

    private Link<?> getLink(NewsletterIssueDto issue) {
        return (Link<?>) new BookmarkablePageLink<>("issueLink", NewsletterIssuePage.class,
            NewsletterIssuePage.buildShowIssueParams(issue.number))
            .setBody(Model.of(new StringResourceModel("all.issues.link.label")
                .setParameters(issue.number,
                    DATE_FORMATTER.format(issue.publishedDate))));
    }
}
