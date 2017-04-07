package com.jvm_bloggers.core.data_fetching.blogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvm_bloggers.core.data_fetching.blogs.json_data.BloggersData;
import com.jvm_bloggers.entities.blog.BlogType;
import com.jvm_bloggers.entities.metadata.Metadata;
import com.jvm_bloggers.entities.metadata.MetadataKeys;
import com.jvm_bloggers.entities.metadata.MetadataRepository;
import com.jvm_bloggers.utils.NowProvider;
import javaslang.control.Option;
import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@Slf4j
public class BloggersDataFetcher {

    private final Option<URL> bloggersUrlOption;
    private final Option<URL> companiesUrlOption;
    private final Option<URL> videosUrlOption;
    private final BloggersDataUpdater bloggersDataUpdater;
    private final ObjectMapper mapper;
    private final MetadataRepository metadataRepository;
    private final NowProvider nowProvider;
    private final PreventConcurrentExecutionSafeguard concurrentExecutionSafeguard
        = new PreventConcurrentExecutionSafeguard();

    @Autowired
    public BloggersDataFetcher(@Value("${bloggers.data.file.url}") String bloggersDataUrlString,
                               @Value("${companies.data.file.url}") String companiesDataUrlString,
                               @Value("${youtube.data.file.url}") String videosDataUrlString,
                               BloggersDataUpdater bloggersDataUpdater,
                               ObjectMapper mapper, MetadataRepository metadataRepository,
                               NowProvider nowProvider) {
        bloggersUrlOption = convertToUrl(bloggersDataUrlString);
        companiesUrlOption = convertToUrl(companiesDataUrlString);
        videosUrlOption = convertToUrl(videosDataUrlString);
        this.bloggersDataUpdater = bloggersDataUpdater;
        this.mapper = mapper;
        this.metadataRepository = metadataRepository;
        this.nowProvider = nowProvider;
    }

    private Option<URL> convertToUrl(String urlString) {
        return Try
            .of(() -> new URL(urlString))
            .onFailure(exc -> log.error("Invalid URL " + urlString))
            .getOption();
    }

    public void refreshData() {
        concurrentExecutionSafeguard.preventConcurrentExecution(this::startFetchingProcess);
    }

    @Async("singleThreadExecutor")
    public void refreshDataAsynchronously() {
        refreshData();
    }

    private Void startFetchingProcess() {
        refreshBloggersDataFor(bloggersUrlOption, BlogType.PERSONAL);
        refreshBloggersDataFor(companiesUrlOption, BlogType.COMPANY);
        refreshBloggersDataFor(videosUrlOption, BlogType.VIDEOS);

        final Metadata dateOfLastFetch = metadataRepository
            .findByName(MetadataKeys.DATE_OF_LAST_FETCHING_BLOGGERS);
        dateOfLastFetch.setValue(nowProvider.now().toString());
        metadataRepository.save(dateOfLastFetch);
        return null;
    }

    private void refreshBloggersDataFor(Option<URL> blogsDataUrl, BlogType blogType) {
        if (blogsDataUrl.isDefined()) {
            try {
                BloggersData bloggers = mapper.readValue(blogsDataUrl.get(), BloggersData.class);
                bloggers.getBloggers().forEach(it -> it.setBlogType(blogType));
                UpdateStatistic updateStatistic = bloggersDataUpdater.updateData(bloggers);
                log.info("Refreshed {} blogs: {}", blogType, updateStatistic);
            } catch (Exception exception) {
                log.error("Exception during parse process for {}", blogType, exception);
            }
        } else {
            log.warn("No valid URL specified for {}. Skipping.", blogType);
        }
    }

    public boolean isFetchingProcessInProgress() {
        return concurrentExecutionSafeguard.isExecuting();
    }
}
