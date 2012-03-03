package net.hamnaberg.confluence.atompub;

import com.atlassian.confluence.pages.Attachment;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author Erlend Hamnaberg<erlend.hamnaberg@arktekk.no>
 */
public class AttachmentFeed {
    private ConfluenceServices services;

    public AttachmentFeed(ConfluenceServices services) {
        this.services = services;
    }

    Feed buildFeed(List<Attachment> attachments, UriInfo info) {
        Feed feed = Abdera.getInstance().newFeed();
        feed.newId();
        feed.addAuthor("Confluence");
        feed.addLink(info.getRequestUri().toString(), Link.REL_SELF);
        for (Attachment attachment : attachments) {
            feed.addEntry(buildEntry(attachment));
        }
        return feed;
    }

    private Entry buildEntry(Attachment attachment) {
        Entry entry = Abdera.getInstance().newEntry();
        entry.setId("urn:id:" + attachment.getId());
        entry.setPublished(attachment.getCreationDate());
        entry.setUpdated(attachment.getLastModificationDate());
        entry.setEdited(attachment.getLastModificationDate());
        entry.setTitle(attachment.getDisplayTitle());
        entry.setSummary(attachment.getComment());
        entry.setContent(downloadIRI(attachment, services), attachment.getContentType());
        return entry;
    }

    static void createEnclosureLink(Attachment attachment, Entry entry, ConfluenceServices services) {
        IRI downloadURI = downloadIRI(attachment, services);
        Link link = entry.addLink(downloadURI.toString(), Link.REL_ENCLOSURE);
        link.setLength(attachment.getFileSize());
        link.setTitle(attachment.getFileName());
        link.setMimeType(attachment.getContentType());
    }

    private static IRI downloadIRI(Attachment attachment, ConfluenceServices services) {
        String baseUrl = services.getSettingsManager().getGlobalSettings().getBaseUrl();
        return new IRI(UriBuilder.fromUri(baseUrl).path(attachment.getDownloadPath()).build());
    }
}
