AJS.toInit(function() {
    var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");

    function populateForm() {
        AJS.$.ajax({
            url: baseUrl + "/rest/atompub-admin/latest/",
            dataType: "json",
            success: function(config) {
                AJS.$("#page_feed_ttl").attr("value", config.page.ttl);
                AJS.$("#page_ttl").attr("value", config.page_feed.ttl);
                AJS.$("#news_feed_ttl").attr("value", config.news_feed.ttl);
                AJS.$("#news_ttl").attr("value", config.news.ttl);
                AJS.$("#page_must_revalidate").attr("value", config.page.revalidate);
                AJS.$("#news_must_revalidate").attr("value", config.news.revalidate);
            }
        });
    }

    populateForm();
});

function updateConfig() {
    AJS.$.ajax({
        url: baseUrl + "/rest/xproduct-admin/latest/",
        type: "PUT",
        contentType: "application/json",
        data: '{ "name": "' + AJS.$("#name").attr("value") + '", "time": ' +  AJS.$("#time").attr("value") + ' }',
        processData: false
    });
}

AJS.$("#admin").submit(function(e) {
    e.preventDefault();
    updateConfig();
});
