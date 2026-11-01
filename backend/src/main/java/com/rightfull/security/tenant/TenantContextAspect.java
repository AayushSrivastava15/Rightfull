package com.rightfull.security.tenant;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AOP Aspect ensuring that active tenant context is automatically bound
 * to the PostgreSQL transaction session upon entering transactional workspace services.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantContextAspect {

    private final TenantContextManager tenantContextManager;

    public TenantContextAspect(TenantContextManager tenantContextManager) {
        this.tenantContextManager = tenantContextManager;
    }

    @Before("execution(* com.rightfull.workspace.service.*.*(..)) || execution(* com.rightfull.identity.service.*.*(..)) || execution(* com.rightfull.product.service.*.*(..))")
    public void bindTenantContextToTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TenantContext.get().ifPresent(ctx ->
                    tenantContextManager.applyTenantContext(ctx.workspaceId(), ctx.userId(), ctx.supabaseUserId(), ctx.email())
            );
        }
    }
}

