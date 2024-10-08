package com.github.yulichang.toolkit;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.yulichang.adapter.AdapterHelper;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.github.yulichang.interceptor.pagination.PageInnerInterceptorWrapper;
import org.apache.ibatis.plugin.Interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * mybatis 拦截器列表
 * 用于替换 interceptorChain 中的拦截器列表
 * 保证 MPJInterceptor 再最后一个（第一个执行）
 *
 * @author yulichang
 * @since 1.3.0
 */
public class InterceptorList<E extends Interceptor> extends ArrayList<E> {

    public InterceptorList() {
        super();
    }

    public InterceptorList(Collection<? extends E> c) {
        super(c);
        Predicate<E> predicate = i -> i instanceof MPJInterceptor;
        if (this.stream().anyMatch(predicate)) {
            E mpjInterceptor = super.stream().filter(predicate).findFirst().orElse(null);
            super.removeIf(predicate);
            super.add(mpjInterceptor);
        }
        this.forEach(this::wrapperInnerPage);
    }

    @Override
    public boolean add(E e) {
        this.wrapperInnerPage(e);
        if (this.isEmpty()) {
            return super.add(e);
        }
        boolean add = super.add(e);
        Predicate<E> predicate = i -> i instanceof MPJInterceptor;
        if (this.stream().anyMatch(predicate)) {
            E mpjInterceptor = super.stream().filter(predicate).findFirst().orElse(null);
            super.removeIf(predicate);
            return super.add(mpjInterceptor);
        }
        return add;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        c.forEach(this::wrapperInnerPage);
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        c.forEach(this::wrapperInnerPage);
        return super.addAll(index, c);
    }

    private void wrapperInnerPage(Interceptor interceptor) {
        AdapterHelper.getAdapter().wrapperInnerPage(interceptor, p -> p instanceof PageInnerInterceptorWrapper,
                p -> new PageInnerInterceptorWrapper((PaginationInnerInterceptor) p));
    }
}
