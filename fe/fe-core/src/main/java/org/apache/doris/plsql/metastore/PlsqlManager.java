// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.plsql.metastore;

import org.apache.doris.catalog.Env;
import org.apache.doris.common.io.Text;
import org.apache.doris.common.io.Writable;
import org.apache.doris.persist.gson.GsonUtils;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

public class PlsqlManager implements Writable {
    private static final Logger LOG = LogManager.getLogger(PlsqlManager.class);

    @SerializedName(value = "nameToStoredProcedures")
    Map<PlsqlProcedureKey, PlsqlStoredProcedure> nameToStoredProcedures = Maps.newConcurrentMap();

    @SerializedName(value = "nameToPackages")
    Map<PlsqlProcedureKey, PlsqlPackage> nameToPackages = Maps.newConcurrentMap();

    public PlsqlManager() {
    }

    public PlsqlStoredProcedure getPlsqlStoredProcedure(PlsqlProcedureKey plsqlProcedureKey) {
        return nameToStoredProcedures.get(plsqlProcedureKey);
    }

    public void addPlsqlStoredProcedure(PlsqlStoredProcedure procedure, boolean isForce) {
        PlsqlProcedureKey plsqlProcedureKey = new PlsqlProcedureKey(procedure.getName(), procedure.getCatalogName(),
                procedure.getDbName());
        if (isForce) {
            nameToStoredProcedures.put(plsqlProcedureKey, procedure);
        } else if (nameToStoredProcedures.putIfAbsent(plsqlProcedureKey, procedure) != null) {
            throw new RuntimeException(plsqlProcedureKey + ", stored procedure already exist.");
        }
        Env.getCurrentEnv().getEditLog().logAddPlsqlStoredProcedure(procedure);
        LOG.info("Add stored procedure success: {}", plsqlProcedureKey);
    }

    public void replayAddPlsqlStoredProcedure(PlsqlStoredProcedure procedure) {
        PlsqlProcedureKey plsqlProcedureKey = new PlsqlProcedureKey(procedure.getName(), procedure.getCatalogName(),
                procedure.getDbName());
        nameToStoredProcedures.put(plsqlProcedureKey, procedure);
        LOG.info("Replay add stored procedure success: {}", plsqlProcedureKey);
    }

    public void dropPlsqlStoredProcedure(PlsqlProcedureKey plsqlProcedureKey) {
        nameToStoredProcedures.remove(plsqlProcedureKey);
        Env.getCurrentEnv().getEditLog().logDropPlsqlStoredProcedure(plsqlProcedureKey);
        LOG.info("Drop stored procedure success: {}", plsqlProcedureKey);
    }

    public void replayDropPlsqlStoredProcedure(PlsqlProcedureKey plsqlProcedureKey) {
        nameToStoredProcedures.remove(plsqlProcedureKey);
        LOG.info("Replay drop stored procedure success: {}", plsqlProcedureKey);
    }

    public PlsqlPackage getPackage(PlsqlProcedureKey plsqlProcedureKey) {
        return nameToPackages.get(plsqlProcedureKey);
    }

    public void addPackage(PlsqlPackage pkg, boolean isForce) {
        PlsqlProcedureKey plsqlProcedureKey = new PlsqlProcedureKey(pkg.getName(), pkg.getCatalogName(),
                pkg.getDbName());
        nameToPackages.put(plsqlProcedureKey, pkg);
        if (isForce) {
            nameToPackages.put(plsqlProcedureKey, pkg);
        } else if (nameToPackages.putIfAbsent(plsqlProcedureKey, pkg) != null) {
            throw new RuntimeException(plsqlProcedureKey + ", package already exist.");
        }
        Env.getCurrentEnv().getEditLog().logAddPlsqlPackage(pkg);
        LOG.info("Add plsql package success: {}", plsqlProcedureKey);
    }

    public void replayAddPlsqlPackage(PlsqlPackage pkg) {
        PlsqlProcedureKey plsqlProcedureKey = new PlsqlProcedureKey(pkg.getName(), pkg.getCatalogName(),
                pkg.getDbName());
        nameToPackages.put(plsqlProcedureKey, pkg);
        LOG.info("Replay add plsql package success: {}", plsqlProcedureKey);
    }

    public void dropPackage(PlsqlProcedureKey plsqlProcedureKey) {
        nameToPackages.remove(plsqlProcedureKey);
        Env.getCurrentEnv().getEditLog().logDropPlsqlPackage(plsqlProcedureKey);
        LOG.info("Drop plsql package success: {}", plsqlProcedureKey);
    }

    public void replayDropPlsqlPackage(PlsqlProcedureKey plsqlProcedureKey) {
        nameToPackages.remove(plsqlProcedureKey);
        LOG.info("Replay drop plsql package success: {}", plsqlProcedureKey);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        String json = GsonUtils.GSON.toJson(this);
        Text.writeString(out, json);
    }

    public static PlsqlManager read(DataInput in) throws IOException {
        String json = Text.readString(in);
        return GsonUtils.GSON.fromJson(json, PlsqlManager.class);
    }
}
