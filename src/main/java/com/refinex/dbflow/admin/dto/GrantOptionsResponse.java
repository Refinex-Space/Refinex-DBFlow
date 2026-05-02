package com.refinex.dbflow.admin.dto;

import com.refinex.dbflow.admin.view.GrantEnvironmentOption;
import com.refinex.dbflow.admin.view.UserOption;

import java.util.List;

/**
 * React 管理端授权选项响应。
 *
 * @param users        active 用户选项
 * @param environments 可授权环境选项
 * @author refinex
 */
public record GrantOptionsResponse(List<UserOption> users, List<GrantEnvironmentOption> environments) {
}
