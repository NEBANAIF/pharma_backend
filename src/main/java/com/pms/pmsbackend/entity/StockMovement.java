package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch; // nullable -- some movements (e.g. a multi-batch sale line) aren't tied to a single batch

    // SALE, RETURN, PURCHASE, TRANSFER_IN, TRANSFER_OUT, DISCARD, ADJUSTMENT
    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    // Signed: positive for additions/returns, negative for deductions/discards.
    // Stored redundantly (rather than always recomputed) so history reads
    // don't need to redo the subtraction, and so it survives even if the
    // before/after semantics of a future source type ever differ.
    @Column(nullable = false)
    private Integer change;

    // e.g. "Sale #42", "Return #7", "Discard: expired", free text
    private String reference;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
