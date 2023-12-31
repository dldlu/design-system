package edu.dlu.bysj.common.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.dlu.bysj.base.model.entity.*;
import edu.dlu.bysj.base.model.query.TopicApprovalListQuery;
import edu.dlu.bysj.base.model.vo.*;
import edu.dlu.bysj.base.result.CommonResult;
import edu.dlu.bysj.base.util.JwtUtil;
import edu.dlu.bysj.common.service.*;
import edu.dlu.bysj.log.annotation.LogAnnotation;
import edu.dlu.bysj.system.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author XiangXinGang
 * @date 2021/10/24 9:40
 */
@RestController
@RequestMapping(value = "/common")
@Api(tags = "公共信息控制器")
@Validated
public class InformationController {
    private final TeacherService teacherService;

    private final StudentService studentService;

    private final SubjectService subjectService;

    private final TitleService titleService;

    private final DegreeService degreeService;

    private final OfficeService officeService;

    private final ClassService classService;

    private final SchoolService schoolService;

    private final FunctionService functionService;

    private final MajorService majorService;

    private final CollegeService collegeService;


    @Autowired
    public InformationController(FunctionService functionService, SchoolService schoolService,
                                 ClassService classService, OfficeService officeService,
                                 DegreeService degreeService, TitleService titleService,
                                 SubjectService subjectService, StudentService studentService,
                                 TeacherService teacherService, MajorService majorService,
                                 CollegeService collegeService) {
        this.collegeService = collegeService;
        this.functionService = functionService;
        this.schoolService = schoolService;
        this.classService = classService;
        this.officeService = officeService;
        this.degreeService = degreeService;
        this.titleService = titleService;
        this.subjectService = subjectService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.majorService = majorService;
    }

    @GetMapping(value = "/teacherInformation")
    @LogAnnotation(content = "获取教师信息")
    @RequiresPermissions({"common:teacherInformation"})
    @ApiOperation(value = "获取本专业教师信息")
    public CommonResult<List<TeacherDetailVo>> teacherInformation(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        List<TeacherDetailVo> result = null;
        if (!StringUtils.isEmpty(jwt)) {
            result = teacherService.getTeacherInfo(JwtUtil.getMajorId(jwt));
        }
        return CommonResult.success(result);
    }

    /*
     * @Description: 用户选择专业后获取本专业的教师
     * @Author: sinre
     * @Date: 2022/6/18 18:19
     * @param majorId
     * @return edu.dlu.bysj.base.result.CommonResult<java.util.List<edu.dlu.bysj.base.model.vo.TeacherDetailVo>>
     **/
    @GetMapping(value = "/teacherInfoByMajorId")
    @LogAnnotation(content = "获取教师信息")
    @RequiresPermissions({"common:teacherInformation"})
    @ApiOperation(value = "根据专业id获取教师信息")
    public CommonResult<List<TeacherDetailVo>> teacherInformation(
            @Valid @NotNull(message = "专业id不能为空") Integer majorId) {
        return CommonResult.success(teacherService.getTeacherInfo(majorId));
    }

    @GetMapping(value = "/teacherInfoByCollegeId")
    @LogAnnotation(content = "获取教师信息")
    @RequiresPermissions({"common:teacherInformation"})
    @ApiOperation(value = "根据学院id获取教师信息")
    public CommonResult<List<TeacherDetailVo>> teacherCollegeInformation(@NotNull(message = "专业id不能为空") Integer collegeId) {
        return CommonResult.success(teacherService.getCollegeTeacherInfo(collegeId));
    }

    /*
     * @Description: 获取本学院教师信息
     * @Author: sinre
     * @Date: 2022/6/18 19:11
     * @param request
     * @return edu.dlu.bysj.base.result.CommonResult<java.util.List<edu.dlu.bysj.base.model.vo.UserVo>>
     **/
    @GetMapping(value = "/collegeTeacher")
    @LogAnnotation(content = "获取教师信息")
    @RequiresPermissions({"common:teacherInformation"})
    @ApiOperation(value = "获取本学院教师信息")
    public CommonResult<List<UserVo>> collegeTeacher(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        Integer majorId = JwtUtil.getMajorId(jwt);
        Major major = majorService.getOne(new QueryWrapper<Major>().eq("id", majorId));
        Integer collegeId = major.getCollegeId();
        
        List<UserVo> list = majorService.obtainCollegeTeacher(collegeId);

        return CommonResult.success(list);
    }

    @GetMapping(value = "/userCollegeMajor")
    @LogAnnotation(content = "获取学院专业信息")
    @RequiresPermissions({"common:collegeMajor"})
    @ApiOperation(value = "获取学院专业信息")
    public CommonResult<TotalPackageVo<MajorVo>> userCollegeMajor(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        Integer majorId = JwtUtil.getMajorId(jwt);
        Integer collegeIdByMajorId = collegeService.getCollegeIdByMajorId(majorId);
        TotalPackageVo<MajorVo> totalPackageVo =
                majorService.majorPagination(collegeIdByMajorId, 1, 100);
        return CommonResult.success(totalPackageVo);
    }

    @GetMapping(value = "/studentInformation")
    @LogAnnotation(content = "获取学生个人详细信息")
    @RequiresPermissions({"common:studentInformation"})
    @ApiOperation(value = "获取学生个人信息")
    public CommonResult<StudentDetailVo> studentInformation(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        StudentDetailVo detailVo = null;
        if (!StringUtils.isEmpty(jwt)) {
            detailVo = studentService.checkStudentInfo(JwtUtil.getUserId(jwt));
            detailVo.setUserId(JwtUtil.getUserId(jwt));
        }
        return CommonResult.success(detailVo);
    }

    /*
     * @Description: 获取学生个人详细信息
     * @Author: sinre
     * @Date: 2022/6/18 20:07
     * @param studentNumber
     * @return edu.dlu.bysj.base.result.CommonResult<edu.dlu.bysj.base.model.vo.StudentDetailVo>
     **/
    @GetMapping(value = "/studentInformationByNumber")
    @LogAnnotation(content = "获取学生个人详细信息")
    @RequiresPermissions({"common:studentInformation"})
    @ApiOperation(value = "通过Id获取学生个人信息")
    public CommonResult<StudentDetailVo> studentInformationByNumber(
            @Valid @NotNull(message = "学生信息不能为空") Integer studentNumber) {
        StudentDetailVo detailVo = null;
        if (studentNumber.toString().length() == 8)
            detailVo = studentService.checkStudentInfoByNumber(studentNumber);
        return CommonResult.success(detailVo);
    }

    @GetMapping(value = "/teacherSelfInformationByNumber")
    @LogAnnotation(content = "获取教师个人详细信息")
    @RequiresPermissions({"common:teacherInformation"})
    @ApiOperation(value = "获取教师个人信息")
    public CommonResult<TeacherDetailVo> teacherInformationByNumber(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        String userNumber = JwtUtil.getUserNumber(jwt);
        Teacher teacher = teacherService.getOne(new QueryWrapper<Teacher>().eq("teacher_number",userNumber));
        Major major = majorService.getById(teacher.getMajorId());
        TeacherTitle title = titleService.getById(teacher.getTitleId());
        Office office = officeService.getById(teacher.getOfficeId());
        Integer collegeId = collegeService.getCollegeIdByMajorId(teacher.getMajorId());
        College college = collegeService.getById(collegeId);
        TeacherDetailVo detailVo = new TeacherDetailVo();
        detailVo.setSex(teacher.getSex());
        detailVo.setCollegeId(college.getId());
        detailVo.setUserId(teacher.getId());
        detailVo.setCollege(college.getName());
        detailVo.setMajorId(major.getId());
        detailVo.setOffice(office.getName());
        detailVo.setTitle(title.getName());
        detailVo.setTeacherNumber(teacher.getTeacherNumber());
        detailVo.setUsername(teacher.getName());
        detailVo.setMajorName(major.getName());
        detailVo.setPhone(teacher.getPhoneNumber());
        detailVo.setEmail(teacher.getEmail());
        detailVo.setCanUse(teacher.getCanUse());
        return CommonResult.success(detailVo);
    }

    @GetMapping(value = "/studentSubject")
    @LogAnnotation(content = "查看学生题目信息")
    @RequiresPermissions({"common:studentSubject"})
    @ApiOperation(value = "获取学生题目信息")
    public CommonResult<SubjectSimplifyVo> studentSubjectInformation(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        SubjectSimplifyVo subjectSimplifyVo = null;
        if (!StringUtils.isEmpty(jwt)) {
            subjectSimplifyVo = subjectService.studentSimplifyInfo(JwtUtil.getUserId(jwt));
        }
        return CommonResult.success(subjectSimplifyVo);
    }

    @GetMapping(value = "/subjectInfoById")
    @LogAnnotation(content = "查看学生题目信息")
    @RequiresPermissions({"common:studentSubject"})
    @ApiOperation(value = "获取学生题目信息")
    public CommonResult<Map<String,String>> subjectInfoById(@Valid @NotNull(message = "专业id不能为空") Integer subjectId) {
        return CommonResult.success(subjectService.obtainsSubjectInfoById(subjectId));
    }

    @GetMapping(value = "/subjectDetailById")
    @LogAnnotation(content = "查看学生题目信息")
    @RequiresPermissions({"common:studentSubject"})
    @ApiOperation(value = "获取学生题目信息")
    public CommonResult<SubjectTableVo> subjectDetailById(
            @Valid @NotBlank(message = "题目信息不能为空") String subjectId) {
        SubjectTableVo subjectTableVo = subjectService.obtainsSubjectTableInfo(subjectId);
        if (ObjectUtil.isNull(subjectTableVo)) {
            Integer id = subjectService.getOne(new QueryWrapper<Subject>().eq("subject_id", subjectId)).getId();
            subjectTableVo = subjectService.obtainsSubjectTableInfo(id.toString());
        }
        return CommonResult.success(subjectTableVo);
    }

    @GetMapping(value = "/taskbook/keyAndMajorList")
    @LogAnnotation(content = "获取任务书审批表")
    @RequiresPermissions({"common:subjectAudits"})
    @ApiOperation(value = "按关键词和专业获取任务书审批表")
    public CommonResult<TotalPackageVo<ApprovalTaskBookVo>> topicApprovalByCondition(
            TopicApprovalListQuery query) {
        TotalPackageVo<ApprovalTaskBookVo> result = null;
        if (ObjectUtil.isNotNull(query)) {
            result = subjectService.subjectApprovalInfo(query);
        }
        return CommonResult.success(result);
    }

    @GetMapping(value = "/title")
    @LogAnnotation(content = "获取职称列表")
    @RequiresPermissions({"common:teacherTitle"})
    @ApiOperation(value = "获取职称列表")
    public CommonResult<List<Map<String, Object>>> titleList() {
        List<Map<String, Object>> maps = titleService.allTitleSimplify();
        return CommonResult.success(maps);
    }

    @GetMapping(value = "/degree")
    @LogAnnotation(content = "获取学位列表")
    @RequiresPermissions({"common:teacherDegree"})
    @ApiOperation(value = "获取学位列表")
    public CommonResult<List<DegreeSimplifyVo>> degreeList() {
        List<DegreeSimplifyVo> result = degreeService.allDegreeSimplifyInfo();
        return CommonResult.success(result);
    }

    @GetMapping(value = "/office")
    @LogAnnotation(content = "获取科室列表")
    @RequiresPermissions({"common:teacherOffice"})
    @ApiOperation(value = "获取科室列表")
    public CommonResult<List<OfficeSimplifyVo>> officeList() {
        List<OfficeSimplifyVo> officeSimplifyVos = officeService.officeSimplifyInfo();
        return CommonResult.success(officeSimplifyVos);
    }


    @GetMapping(value = "/majorClass")
    @LogAnnotation(content = "获取本专业的所有班级")
    @RequiresPermissions({"common:majorClass"})
    @ApiOperation(value = "获取本专业的所有班级")
    public CommonResult<List<ClassSimplifyVo>> majorClassList(Integer majorId, HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        List<ClassSimplifyVo> result = null;
        if (!StringUtils.isEmpty(jwt)) {
            result = classService.classSimplifyInfo(majorId);
        }
        return CommonResult.success(result);
    }


    @GetMapping(value = "/role")
    @LogAnnotation(content = "获取该用户角色集合")
    @RequiresPermissions({"common:teacherRole"})
    @ApiOperation(value = "获取该人所属的角色集合")
    public CommonResult<List<RoleSimplifyVo>> userRoleList(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        List<RoleSimplifyVo> result = null;
        if (!StringUtils.isEmpty(jwt)) {
            result = teacherService.teacherRoleNameAndId(JwtUtil.getRoleIds(jwt));
        }
        return CommonResult.success(result);
    }


    @GetMapping(value = "/school")
    @LogAnnotation(content = "获取学校基本信息列表")
    @RequiresPermissions({"common:schools"})
    @ApiOperation(value = "获取学校基本信息列表")
    public CommonResult<List<SchoolSimplifyVo>> schoolList() {
        List<SchoolSimplifyVo> schoolSimplifyVoList = schoolService.schoolInfo();
        return CommonResult.success(schoolSimplifyVoList);
    }

    @GetMapping(value = "/functionList")
    @LogAnnotation(content = "获取该人的菜单信息")
    @RequiresPermissions({"common:functionLists"})
    @ApiOperation(value = "获取该人的functionTime")
    public CommonResult<List<FunctionSimplifyVo>> personFunctionList(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        List<FunctionSimplifyVo> result = null;
        if (!StringUtils.isEmpty(jwt)) {
            String now = DateUtil.now();
            result = functionService.personFunctionByTime(JwtUtil.getRoleIds(jwt), now);
        }
        return CommonResult.success(result);
    }
}
