package com.mysiteforme.admin.controller.system;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysiteforme.admin.annotation.SysLog;
import com.mysiteforme.admin.base.BaseController;
import com.mysiteforme.admin.entity.VO.TableField;
import com.mysiteforme.admin.entity.VO.TableVO;
import com.mysiteforme.admin.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * @Author:tnt
 * @Description:${TODO}
 * @Date: Create in 17:51 2017/12/25.
 */
@Controller
@RequestMapping("admin/system/table")
public class TableController extends BaseController{

    private static final Logger LOGGER = LoggerFactory.getLogger(TableController.class);

    private static final String[] keywords = {"public","protected","private","protected","class","interface","abstract","implements","extends","new",
    "import","package","byte","char","boolean","short","int","float","long","double","void","null","true","false","if","else","while","for","switch",
    "case","default","do","break","continue","return","instanceof","static","final","super","this","native","strictfp","synchronized","transient","volatile",
    "catch","try","finally","throw","throws","enum","assert","throw","throws","this"};

    @Autowired
    private CreateTableFiles createTableFiles;
    /**
     *
     * @return
     */
    @GetMapping("list")
    @SysLog("???????????????????????????")
    public String list(){
        return "admin/system/table/list";
    }

    /***
     * ???????????????????????????
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @RequiresPermissions("sys:table:list")
    @PostMapping("list")
    @ResponseBody
    public LayerData<TableVO> list(@RequestParam(value = "page",defaultValue = "1")Integer page,
                                    @RequestParam(value = "limit",defaultValue = "10")Integer limit,
                                    ServletRequest request){
        Map<String,Object> map = WebUtils.getParametersStartingWith(request, "s_");
        LayerData<TableVO> tableLayerData = new LayerData<>();
        if(!map.isEmpty()){
            String name = (String) map.get("name");
            if(StringUtils.isBlank(name)) {
                map.remove("name");
            }
        }
        Page<TableVO> tablePage = tableService.selectTablePage(new Page<>(page,limit),map);
        tableLayerData.setCount(tablePage.getTotal());
        tableLayerData.setData(tablePage.getRecords());
        return tableLayerData;
    }

    @GetMapping("add")
    public String add(){
        return "admin/system/table/add";
    }

    @RequiresPermissions("sys:table:add")
    @PostMapping("add")
    @ResponseBody
    @SysLog("???????????????????????????")
    public RestResponse add(@RequestBody TableVO tableVO){
        if(StringUtils.isBlank(tableVO.getName())){
            return RestResponse.failure("??????????????????");
        }
        if(ArrayUtils.contains(keywords,tableVO.getName())){
            return RestResponse.failure("????????????java?????????");
        }
        if(StringUtils.isBlank(tableVO.getComment())){
            return RestResponse.failure("????????????????????????");
        }
        if(tableVO.getFieldList() == null || tableVO.getFieldList().size() == 0){
            return RestResponse.failure("????????????????????????");
        }
        if(tableService.existTable(tableVO.getName().toLowerCase())>0){
            return RestResponse.failure("?????????????????????"+tableVO.getName()+"???????????????");
        }
        String entityName = "dict,group,menu,role,site,user,table,file";
        if(entityName.equals(tableVO.getName().toLowerCase())){
            return RestResponse.failure("??????????????????"+tableVO.getName()+"?????????????????????");
        }
        tableService.creatTable(tableVO);
        return RestResponse.success();
    }

    @GetMapping("edit")
    public String edit(String name,Model model){
        TableVO tableVO = tableService.detailTable(name);
        String[] comments = tableVO.getComment().split(",");
        tableVO.setComment(comments[0]);
        Integer tabletype = comments.length>1?Integer.valueOf(comments[1]):0;
        tableVO.setTabletype(tabletype);

        String base  = "id,create_by,create_date,update_by,update_date,del_flag";
        String tree  = "id,parent_id,level,parent_ids,sort,create_by,create_date,update_by,update_date,del_flag";

        List<String> allNames = Lists.newArrayList();
        for (TableField t : tableVO.getFieldList()){
            if(tabletype == 1){
                if(!base.contains(t.getName())){
                    changeTableField(t);
                    allNames.add(t.getName());
                }
            }
            if(tabletype == 2){
                if(!tree.contains(t.getName())){
                    changeTableField(t);
                    allNames.add(t.getName());
                }
            }
        }
        model.addAttribute("tableVO",tableVO);
        model.addAttribute("allNames",JSONObject.toJSONString(allNames));
        return "admin/system/table/edit";
    }

    @RequiresPermissions("sys:table:edit")
    @PostMapping("editTable")
    @ResponseBody
    @SysLog("???????????????????????????")
    public RestResponse editTable(@RequestParam(value = "name",required = false)String name,
                                  @RequestParam(value = "oldname",required = false)String oldname,
                                  @RequestParam(value = "comment",required = false)String comment,
                                  @RequestParam(value = "tabletype",required = false)Integer tableType){
        if(StringUtils.isNotBlank(name)){
            if(StringUtils.isBlank(oldname)){
                return RestResponse.failure("???????????????????????????");
            }
            if(ArrayUtils.contains(keywords,name)){
                return RestResponse.failure("????????????java?????????");
            }
            if(!name.equals(oldname)){
                if(tableService.existTable(name)>0){
                    return RestResponse.failure("??????????????????????????????");
                }
                tableService.changeTableName(name,oldname,comment,tableType);
            }
        }
        if(StringUtils.isNotBlank(comment)){
            if(tableType == null){
                return RestResponse.failure("???????????????????????????");
            }
        }
        tableService.changeTableComment(name,comment,tableType);
        return RestResponse.success();
    }

    @RequiresPermissions("sys:table:list")
    @PostMapping("fieldlist")
    @ResponseBody
    @SysLog("????????????????????????(????????????)")
    public LayerData<TableField> fieldlist(@RequestParam(value = "page",defaultValue = "1")Integer page,
                                           @RequestParam(value = "limit",defaultValue = "10")Integer limit,
                                           ServletRequest request){
        Map<String,Object> map = WebUtils.getParametersStartingWith(request, "s_");
        if(!map.isEmpty()){
            String name = (String) map.get("name");
            if(StringUtils.isBlank(name)) {
                map.remove("name");
            }
            String tableType = (String)map.get("tableType");
            if(!tableType.equals("1") && !tableType.equals("2") && !tableType.equals("3")){
                map.remove("tableType");
            }else{
                map.put("tableType",Integer.valueOf(tableType));
            }
        }
        Page<TableField> tablePage = tableService.selectTableFieldPage(new Page<>(page,limit),map);
        LayerData<TableField> layerData = new LayerData<>();
        layerData.setData(tablePage.getRecords());
        layerData.setCount(tablePage.getTotal());
        return layerData;
    }

    private void changeTableField(TableField t){
        String[] c = t.getComment().split(",");
        t.setComment(c[0]);
        if(c.length>1) {
            t.setDofor(c[1]);
        }
        if(c.length>3){
            t.setDefaultValue(Boolean.valueOf(c[3]));
        }else {
            t.setDefaultValue(false);
        }
        if(c.length>4){
            t.setListIsShow(Boolean.valueOf(c[4]));
        }
        if(c.length>5){
            t.setListIsSearch(Boolean.valueOf(c[5]));
        }
    }

    @RequiresPermissions("sys:table:list")
    @PostMapping("showFields")
    @ResponseBody
    @SysLog("????????????????????????(????????????)")
    public LayerData<TableField> showFields(@RequestParam(value = "s_name",required = false)String name,
                                            @RequestParam(value = "s_tableType",required = false)Integer tableType){
        LayerData<TableField> tableLayerData = new LayerData<>();
        if(StringUtils.isBlank(name)){
            return null;
        }
        if(tableType != 1 && tableType != 2 && tableType != 3){
            return null;
        }
        Map<String,Object> map = Maps.newHashMap();
        map.put("name",name);
        map.put("tableType",tableType);
        List<TableField> list = tableService.selectFields(map);
        for(TableField t : list){
            changeTableField(t);
        }
        tableLayerData.setData(list);
        tableLayerData.setCount(list.size());
        return tableLayerData;
    }

    @RequiresPermissions("sys:table:addField")
    @PostMapping("addField")
    @ResponseBody
    @SysLog("??????????????????????????????")
    public RestResponse addField(@RequestBody TableField tableField){
        if(StringUtils.isBlank(tableField.getName())){
            return RestResponse.failure("????????????????????????");
        }
        if(ArrayUtils.contains(keywords,tableField.getName())){
            return RestResponse.failure("??????????????????java?????????");
        }
        if(StringUtils.isBlank(tableField.getComment())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getDofor())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getIsNullValue())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getType())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getTableName())){
            return RestResponse.failure("???????????????????????????");
        }
        if(tableField.getTableType() == 1) {
            String base = "id,create_by,create_date,update_by,update_date,del_flag";
            if (base.contains(tableField.getName())) {
                return RestResponse.failure("????????????????????????????????????" + tableField.getName() + "???");
            }
        }
        if(tableField.getTableType() == 2) {
            String tree = "id,parent_id,level,parent_ids,sort,create_by,create_date,update_by,update_date,del_flag";
            if (tree.contains(tableField.getName())) {
                return RestResponse.failure("???????????????????????????????????????" + tableField.getName() + "???");
            }
        }
        if(tableField.getTableType() == 3) {
            if("id".equalsIgnoreCase(tableField.getName())){
                return RestResponse.failure("????????????????????????id??????");
            }
        }
        tableService.addColumn(tableField);
        return RestResponse.success();
    }

    @RequiresPermissions("sys:table:editField")
    @PostMapping("editField")
    @ResponseBody
    @SysLog("??????????????????????????????")
    public RestResponse editField(@RequestBody TableField tableField){
        if(StringUtils.isBlank(tableField.getName())){
            return RestResponse.failure("????????????????????????");
        }
        if(ArrayUtils.contains(keywords,tableField.getName())){
            return RestResponse.failure("??????????????????java?????????");
        }
        if(StringUtils.isBlank(tableField.getComment())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getDofor())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getIsNullValue())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getType())){
            return RestResponse.failure("????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getTableName())){
            return RestResponse.failure("???????????????????????????");
        }
        if(StringUtils.isBlank(tableField.getOldName())){
            return RestResponse.failure("???????????????????????????");
        }
        if(tableField.getTableType() == 1) {
            String base = "id,create_by,create_date,update_by,update_date,del_flag";
            if (base.contains(tableField.getName())) {
                return RestResponse.failure("????????????????????????????????????" + tableField.getName() + "???");
            }
        }
        if(tableField.getTableType() == 2) {
            String tree = "id,parent_id,level,parent_ids,sort,create_by,create_date,update_by,update_date,del_flag";
            if (tree.contains(tableField.getName())) {
                return RestResponse.failure("???????????????????????????????????????" + tableField.getName() + "???");
            }
        }
        if(tableField.getTableType() == 3) {
            if("id".equalsIgnoreCase(tableField.getName())){
                return RestResponse.failure("????????????????????????id??????");
            }
        }
        tableService.updateColumn(tableField);
        return RestResponse.success();
    }

    @PostMapping("fieldIsExist")
    @ResponseBody
    public RestResponse fieldIsExist(@RequestParam(value = "fieldName",required = false)String fieldName,
                                    @RequestParam(value = "tableName",required = false)String tableName){
        if(StringUtils.isBlank(fieldName)){
            return RestResponse.failure("?????????????????????");
        }
        if(ArrayUtils.contains(keywords,fieldName)){
            return RestResponse.failure("??????????????????java?????????");
        }
        if(StringUtils.isBlank(tableName)){
            return RestResponse.failure("????????????????????????");
        }
        if(ArrayUtils.contains(keywords,tableName)){
            return RestResponse.failure("?????????????????????java?????????");
        }
        Map<String,Object> map = Maps.newHashMap();
        map.put("fieldName",fieldName);
        map.put("tableName",tableName);
        if(tableService.existTableField(map)>0){
            return RestResponse.failure("??????????????????");
        }
        return RestResponse.success();
    }


    @RequiresPermissions("sys:table:deleteField")
    @PostMapping("deleteField")
    @ResponseBody
    @SysLog("??????????????????")
    public RestResponse deleteField(@RequestParam(value = "fieldName",required = false)String fieldName,
                                    @RequestParam(value = "tableName",required = false)String tableName){
        if(StringUtils.isBlank(fieldName)){
            return RestResponse.failure("?????????????????????");
        }
        if(StringUtils.isBlank(tableName)){
            return RestResponse.failure("????????????????????????");
        }
        tableService.dropTableField(fieldName,tableName);
        return RestResponse.success();
    }

    @RequiresPermissions("sys:table:deleteTable")
    @PostMapping("delete")
    @ResponseBody
    @SysLog("?????????????????????")
    public RestResponse delete(@RequestParam(value = "tableName",required = false)String tableName){
        if(StringUtils.isBlank(tableName)){
            return RestResponse.failure("???????????????????????????");
        }
        if(tableName.contains("sys_")){
            return RestResponse.failure("?????????????????????");
        }
        tableService.dropTable(tableName);
        return RestResponse.success();
    }

    @RequiresPermissions("sys:table:download")
    @PostMapping("download")
    @ResponseBody
    @SysLog("??????JAVA??????")
    public RestResponse download(@RequestParam(required = false)String[] baseTables,
            @RequestParam(required = false)String[] treeTables,
            HttpServletResponse response) throws Exception {
        if(baseTables != null && treeTables != null){
            if(baseTables.length == 0 && treeTables.length == 0){
                return RestResponse.failure("?????????????????????");
            }
        }

        synchronized(this){
            File baseFloder = new File(createTableFiles.baseDic);
            ZipUtil.deleteDir(baseFloder);
            if(baseTables != null && baseTables.length>0){
                createTableFiles.createFile(baseTables,1);
            }
            if(treeTables != null && treeTables.length>0){
                createTableFiles.createFile(treeTables,2);
            }
            File f = new File(createTableFiles.zipFile);
            try {
                if(f.exists()){
                    f.delete();
                }
                com.xiaoleilu.hutool.util.ZipUtil.zip(createTableFiles.baseDic,createTableFiles.zipFile);
            }catch (Exception e){
                e.printStackTrace();
            }
            String filename = new String(f.getName().getBytes("GB2312"),"ISO8859-1");
            BufferedInputStream br = new BufferedInputStream(new FileInputStream(f));
            byte[] buf = new byte[1024];
            int len = 0;
            response.setCharacterEncoding("UTF-8");
            response.setContentLength((int) f.length());
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            OutputStream  out = response.getOutputStream();
            while ((len = br.read(buf)) > 0) out.write(buf, 0, len);
            br.close();
            out.flush();
            out.close();
            f.delete();
        }
        return RestResponse.success();
    }
}
