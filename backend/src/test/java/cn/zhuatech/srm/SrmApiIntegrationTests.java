/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证后端接口默认拒绝匿名访问，权限控制不依赖前端菜单。 */
@SpringBootTest
@AutoConfigureMockMvc
class SrmApiIntegrationTests {
    @Autowired private MockMvc mvc;

    @Test
    void protectedBusinessApiRejectsAnonymousRequest() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/quotas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/portal/orders/list")).andExpect(status().isUnauthorized());
    }
}
