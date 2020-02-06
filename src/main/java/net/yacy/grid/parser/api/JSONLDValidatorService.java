/**
 *  JSONLDValidatorService
 *  Copyright 13.6.2018 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.parser.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.document.Document;
import net.yacy.document.parser.htmlParser;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ClientConnection;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Data;

/**
 * call examples:
 * http://127.0.0.1:8500/yacy/grid/parser/jsonldvalidator.json?etherpad=05cc1575f55de2dc82f20f9010d71358
 * http://127.0.0.1:8500/yacy/grid/parser/jsonldvalidator.json?url=http://ebay.de
 * http://127.0.0.1:8500/yacy/grid/parser/jsonldvalidator.json?url=https://release-8-0-x-dev-224m2by-lj6ob4e22x2mc.eu.platform.sh/test
 */
public class JSONLDValidatorService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 85784631749879L;
    public static final String NAME = "jsonldvalidator";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/parser/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {

        String url = call.get("url", "");
        
        String etherpad = call.get("etherpad", "");
        String etherpad_urlstub = Data.config.getOrDefault("parser.etherpad.urlstub", "");
        String etherpad_apikey = Data.config.getOrDefault("parser.etherpad.apikey", "");

        JSONObject json = new JSONObject(true);
        json.put(ObjectAPIHandler.SUCCESS_KEY, false);
        json.put(ObjectAPIHandler.COMMENT_KEY, "you must submit either a 'etherpad' or 'url' object");
        
        if (url.length() > 0) {
            try {
                byte[] b = ClientConnection.load(url);
                Document[] docs = htmlParser.parse(url, b);

                String s = htmlParser.RDFa2JSONLDExpandString(url, b);
                JSONArray jaExpand = new JSONArray(s);
                JSONArray jaFlatten = new JSONArray(htmlParser.JSONLDExpand2Mode(url, s, JSONLDMode.FLATTEN));
                JSONObject jaCompact = new JSONObject(htmlParser.JSONLDExpand2Mode(url, s, JSONLDMode.COMPACT));
                String compactString = jaCompact.toString(2); // store the compact json-ld into a string because compact2tree is destructive
                JSONObject jaTree = htmlParser.compact2tree(jaCompact);
                
                json.put("ld", docs[0].ld());
                json.put("ldnew-expand", jaExpand);
                json.put("ldnew-flat", jaFlatten);
                json.put("ldnew-compact", new JSONObject(compactString));
                json.put("ldnew-tree", jaTree);
                json.put(ObjectAPIHandler.COMMENT_KEY, "parsing of url content successfull");
            } catch (Throwable e) {
                json.put(ObjectAPIHandler.COMMENT_KEY, "parsing of url content failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (etherpad.length() > 0 && etherpad_urlstub.length() > 0 && etherpad_apikey.length() > 0) {
            try {
                String content = ClientConnection.loadFromEtherpad(etherpad_urlstub, etherpad_apikey, etherpad);
                Document[] docs = htmlParser.parse(content);
                json.put("ld", docs[0].ld());
                json.put(ObjectAPIHandler.COMMENT_KEY, "parsing of etherpad successfull");
            } catch (IOException e) {
                e.printStackTrace();
                json.put(ObjectAPIHandler.COMMENT_KEY, "parsing of etherpad failed: " + e.getMessage());
            }
        }
        
        return new ServiceResponse(json);
    }
    
}
