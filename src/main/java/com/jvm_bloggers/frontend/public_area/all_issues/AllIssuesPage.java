package com.jvm_bloggers.frontend.public_area.all_issues;

import com.jvm_bloggers.domain.query.newsletter_issue_for_listing.NewsletterIssueForListing;
import com.jvm_bloggers.frontend.public_area.AbstractFrontendPage;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import java.time.YearMonth;

@MountPath("all-issues")
public class AllIssuesPage extends AbstractFrontendPage {

    @SpringBean
    private AllIssuesPageBackingBean backingBean;

    public AllIssuesPage() {
        Map<YearMonth, List<NewsletterIssueForListing>> issuesGroupedByMonthYear =
            backingBean.getIssuesGroupedByYearMonth();
        RepeatingView issuesInMonth = new RepeatingView("issuesInMonthPanel");
        issuesGroupedByMonthYear.forEach((monthYear, issues) -> {
            issuesInMonth.add(
                new IssuesInMonthPanel(issuesInMonth.newChildId(), monthYear, issues)
            );
        });
        add(issuesInMonth);
    }

}
