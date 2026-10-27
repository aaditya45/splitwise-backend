package com.splitwise.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"expense", "user"})
public class ExpenseSplit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long splitId;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String splitType; // EQUAL, PERCENTAGE, ITEMIZE

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpenseSplit other)) return false;
        return splitId != null && splitId.equals(other.splitId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
