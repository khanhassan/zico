/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zico.core.services;

import com.jitlogic.zico.core.*;
import com.jitlogic.zico.shared.data.PasswordInfo;
import com.jitlogic.zico.shared.data.SymbolInfo;
import com.jitlogic.zico.shared.data.UserInfo;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Path("/system")
public class SystemService {
    @Inject
    private UserManager userManager;

    @Inject
    private UserContext userContext;

    @Inject
    private TraceTemplateManager templateManager;

    @Inject
    private HostStoreManager hostStoreManager;

    @GET
    @Path("/user/current")
    @Produces(MediaType.APPLICATION_JSON)
    public UserInfo getUser() {
        return userContext.getUser();
    }


    @POST
    @Path("/user/password")
    public void resetPassword(PasswordInfo pw) {
        boolean adminMode = userContext.isInRole("ADMIN");
        String userName = pw.getUsername();

        if (userName != null && !adminMode) {
            throw new ZicoRuntimeException("Insufficient privileges to reset other users password");
        }

        UserInfo user = userContext.getUser();

        if (!adminMode) {
            String chkHash = "MD5:" + ZorkaUtil.md5(pw.getOldPassword());

            String oldHash = user.getPassword();

            if (!chkHash.equals(oldHash)) {
                throw new ZicoRuntimeException("Invalid (old) password.");
            }
        }

        user.setPassword("MD5:" + ZorkaUtil.md5(pw.getNewPassword()));
        userManager.persist(user);
    }


    @GET
    @Path("/tidmap/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SymbolInfo> getTidMap(@PathParam("hostname") String hostname) {
        userManager.checkHostAccess(hostname);
        return hostStoreManager.getTids(hostname).entrySet().stream()
                .map((e) -> new SymbolInfo(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/backup")
    public synchronized void backup() {
        userManager.export();
        templateManager.export();

        for (HostStore h : hostStoreManager.list(null)) {
            h.export();
        }

    }

}
