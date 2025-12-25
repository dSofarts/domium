package ru.domium.documentservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "document_comment",
    indexes = @Index(name = "idx_comment_doc", columnList = "document_id"))
public class DocumentComment {

  @Id @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private DocumentInstance document;

  @Enumerated(EnumType.STRING)
  @Column(name = "author_type", nullable = false)
  private CommentAuthorType authorType;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "text", nullable = false, length = 4000)
  private String text;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() { this.createdAt = Instant.now(); }
}
