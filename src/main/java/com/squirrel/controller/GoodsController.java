package com.squirrel.controller;

import com.squirrel.common.GgeeConst;
import com.squirrel.dto.AjaxResult;
import com.squirrel.pojo.*;
import com.squirrel.service.*;
import com.squirrel.util.DateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/goods")
public class GoodsController {

    private static Log LOG = LogFactory.getLog(GoodsController.class);

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private CatelogService catelogService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentsService commentsService;

    /**
     * 首页显示商品，每一类商品查询6件，根据最新上架排序 key的命名为catelogGoods1、catelogGoods2....
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/homeGoods")
    public ModelAndView homeGoods(HttpServletRequest request) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        modelAndView.addObject("cur_user", cur_user);
        //商品种类数量
        int catelogSize = 7;
        //每个种类显示商品数量
        int goodsSize = 6;
        for (int i = 1; i <= catelogSize; i++) {
            List<Goods> goodsList = null;
            List<GoodsExtend> goodsAndImage = null;
            goodsList = goodsService.getGoodsByCatelogOrderByDate(i, goodsSize);
            goodsAndImage = new ArrayList<GoodsExtend>();
            for (int j = 0; j < goodsList.size(); j++) {
                //将用户信息和image信息封装到GoodsExtend类中，传给前台
                GoodsExtend goodsExtend = new GoodsExtend();
                Goods goods = goodsList.get(j);
                List<Image> images = imageService.getImagesByGoodsPrimaryKey(goods.getId());
                goodsExtend.setGoods(goods);
                goodsExtend.setImages(images);
                goodsAndImage.add(j, goodsExtend);
            }
            String key = "catelog" + "Goods" + i;
            modelAndView.addObject(key, goodsAndImage);
        }
        modelAndView.setViewName("/goods/homeGoods");
        return modelAndView;
    }

    @RequestMapping(value = "/search")
    public ModelAndView searchGoods(HttpServletRequest request, @RequestParam(value = "str", required = false) String str) throws Exception {
        List<Goods> goodsList = goodsService.searchGoods(str, str);
        List<GoodsExtend> goodsExtendList = new ArrayList<GoodsExtend>();
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsExtend goodsExtend = new GoodsExtend();
            Goods goods = goodsList.get(i);
            List<Image> imageList = imageService.getImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(imageList);
            goodsExtendList.add(i, goodsExtend);
        }
        ModelAndView modelAndView = new ModelAndView();
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        modelAndView.addObject("cur_user", cur_user);
        modelAndView.addObject("goodsExtendList", goodsExtendList);
        modelAndView.addObject("search", str);
        modelAndView.setViewName("/goods/searchGoods");
        return modelAndView;
    }

    /**
     * 查询该类商品
     *
     * @param id 要求该参数不为空
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/catelog/{id}")
    public ModelAndView catelogGoods(HttpServletRequest request, @PathVariable("id") Integer id,
                                     @RequestParam(value = "str", required = false) String str) throws Exception {
        List<Goods> goodsList = goodsService.getGoodsByCatelog(id, str, str);
        Catelog catelog = catelogService.selectByPrimaryKey(id);
        List<GoodsExtend> goodsExtendList = new ArrayList<GoodsExtend>();
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsExtend goodsExtend = new GoodsExtend();
            Goods goods = goodsList.get(i);
            List<Image> imageList = imageService.getImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(imageList);
            goodsExtendList.add(i, goodsExtend);
        }
        ModelAndView modelAndView = new ModelAndView();
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        modelAndView.addObject("cur_user", cur_user);
        modelAndView.addObject("goodsExtendList", goodsExtendList);
        modelAndView.addObject("catelog", catelog);
        modelAndView.addObject("search", str);
        modelAndView.setViewName("/goods/catelogGoods");
        return modelAndView;
    }

    /**
     * 根据商品id查询该商品详细信息
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goodsId/{id}")
    public ModelAndView getGoodsById(HttpServletRequest request, @PathVariable("id") Integer id, @RequestParam(value = "str", required = false) String str) throws Exception {
        Goods goods = goodsService.getGoodsByPrimaryKey(id);
        User seller = userService.selectByPrimaryKey(goods.getUserId());
        Catelog catelog = catelogService.selectByPrimaryKey(goods.getCatelogId());
        GoodsExtend goodsExtend = new GoodsExtend();
        List<Image> imageList = imageService.getImagesByGoodsPrimaryKey(id);
        goodsExtend.setGoods(goods);
        goodsExtend.setImages(imageList);
        ModelAndView modelAndView = new ModelAndView();
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        List<Comments> commentsList = commentsService.getCommentsByGoodsId(id);
        Set<Integer> userIds = new HashSet<>();
        for (Comments comments : commentsList) {
            userIds.add(comments.getUserId());
            if (comments.getAtuserId() != 0) {
                userIds.add(comments.getAtuserId());
            }
        }
        List<User> users = userService.getUsersByIds(userIds);
        Map<Integer, User> id2user = users.stream().
                collect(Collectors.toMap(User::getId, user -> user));
        for (Comments comments : commentsList) {
            comments.setUser(id2user.get(comments.getUserId()));
            if (comments.getAtuserId() != 0) {
                comments.setAtuser(id2user.get(comments.getAtuserId()));
            }
        }
        modelAndView.addObject("cur_user", cur_user);
        modelAndView.addObject("goodsExtend", goodsExtend);
        modelAndView.addObject("seller", seller);
        modelAndView.addObject("search", str);
        modelAndView.addObject("catelog", catelog);
        modelAndView.addObject("commentsList", commentsList);
        modelAndView.setViewName("/goods/detailGoods");
        return modelAndView;
    }

    /**
     * 修改商品信息
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/editGoods/{id}")
    public ModelAndView editGoods(HttpServletRequest request, @PathVariable("id") Integer id) throws Exception {

        Goods goods = goodsService.getGoodsByPrimaryKey(id);
        List<Image> imageList = imageService.getImagesByGoodsPrimaryKey(id);
        GoodsExtend goodsExtend = new GoodsExtend();
        goodsExtend.setGoods(goods);
        goodsExtend.setImages(imageList);
        ModelAndView modelAndView = new ModelAndView();
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        modelAndView.addObject("cur_user", cur_user);
        // 将商品信息添加到model
        modelAndView.addObject("goodsExtend", goodsExtend);
        modelAndView.setViewName("/goods/editGoods");
        return modelAndView;
    }

    /**
     * 提交商品更改信息
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/editGoodsSubmit")
    public String editGoodsSubmit(HttpServletRequest request, Goods goods, BindingResult bindingResult) throws Exception {
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        goods.setUserId(cur_user.getId());
        String polish_time = DateUtil.getNowDay();
        goods.setPolishTime(polish_time);
        goodsService.updateGoodsByPrimaryKeyWithBLOBs(goods.getId(), goods);
        return "redirect:/user/allGoods";
    }

    /**
     * 商品下架
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/offGoods")
    public ModelAndView offGoods() throws Exception {

        return null;
    }

    /**
     * 用户删除商品
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/deleteGoods/{id}")
    public String deleteGoods(HttpServletRequest request, @PathVariable("id") Integer id) throws Exception {
        Goods goods = goodsService.getGoodsByPrimaryKey(id);
        //删除商品后，catlog的number-1，user表的goods_num-1，image删除,更新session的值
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        goods.setUserId(cur_user.getId());
        int number = cur_user.getGoodsNum();
        Integer calelog_id = goods.getCatelogId();
        Catelog catelog = catelogService.selectByPrimaryKey(calelog_id);
        catelogService.updateCatelogNum(calelog_id, catelog.getNumber() - 1);
        userService.updateGoodsNum(cur_user.getId(), number - 1);
        cur_user.setGoodsNum(number - 1);
        request.getSession().setAttribute("cur_user", cur_user);//修改session值
        imageService.deleteImagesByGoodsPrimaryKey(id);
        goodsService.deleteGoodsByPrimaryKey(id);
        return "redirect:/user/allGoods";
    }

    /**
     * 发布商品
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/publishGoods")
    public String publishGoods(HttpServletRequest request, Model model) {
        //可以校验用户是否登录
        User cur_user = (User) request.getSession().getAttribute("cur_user");
        if (cur_user == null) {
//            model.addAttribute("cur_user", cur_user);
//            return "/goods/homeGoods";
            return "redirect:/goods/homeGoods";
        } else {
            model.addAttribute("cur_user", cur_user);
            return "/goods/pubGoods";
        }
    }

    /**
     * 提交发布的商品信息
     *
     * @return
     * @throws Exception
     */

    @RequestMapping(value = "/publishGoodsSubmit")
    public String publishGoodsSubmit(HttpServletRequest request, Image ima, Goods goods, BindingResult bindingResult, MultipartFile image)
             {
        //查询出当前用户cur_user对象，便于使用id
        User cur_user = (User) request.getSession().getAttribute("cur_user");

        goods.setUserId(cur_user.getId());
        int i = goodsService.addGood(goods, 10);//在goods表中插入物品
        //返回插入的该物品的id
        int goodsId = goods.getId();
        ima.setGoodsId(goodsId);
        imageService.insert(ima);
        //在image表中插入商品图片
        //发布商品后，catlog的number+1，user表的goods_num+1，更新session的值
        int number = cur_user.getGoodsNum();
        Integer calelog_id = goods.getCatelogId();
        Catelog catelog = catelogService.selectByPrimaryKey(calelog_id);
        catelogService.updateCatelogNum(calelog_id, catelog.getNumber() + 1);
        userService.updateGoodsNum(cur_user.getId(), number + 1);
        cur_user.setGoodsNum(number + 1);
        request.getSession().setAttribute("cur_user", cur_user);//修改session值
        return "redirect:/user/allGoods";
    }

    /**
     * 上传物品
     *
     * @param session
     * @param myfile
     * @return
     * @throws IllegalStateException
     * @throws IOException
     */
    @ResponseBody
    @RequestMapping(value = "/uploadFile")
    public Map<String, Object> uploadFile(HttpSession session, MultipartFile myfile) throws IllegalStateException, IOException {
        //原始名称
        String oldFileName = myfile.getOriginalFilename(); //获取上传文件的原名
        //存储图片的物理路径
        //String file_path = session.getServletContext().getRealPath("upload");
        String file_path = GgeeConst.UPLOAD_FILE_IMAGE_PATH;
        LOG.info("file_path = " + file_path);
        //上传图片
        if (myfile != null && oldFileName != null && oldFileName.length() > 0) {
            //新的图片名称
            String newFileName = UUID.randomUUID() + oldFileName.substring(oldFileName.lastIndexOf("."));
            //新图片
            //File newFile = new File(file_path+"/"+newFileName);
            File newFile = new File(file_path + newFileName);
            //将内存中的数据写入磁盘
            myfile.transferTo(newFile);
            //将新图片名称返回到前端
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("success", "成功啦");
            map.put("imgUrl", newFileName);
            return map;
        } else {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("error", "图片不合法");
            return map;
        }
    }

    //更新商品信息
    @RequestMapping(value = "/api/updateGoods", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult updateGoods(@RequestBody Goods goods) {
        AjaxResult ajaxResult = new  AjaxResult();
        goodsService.updateGoodsByPrimaryKeyWithBLOBs(
                goods.getId(), goods);
        return new AjaxResult().setData(true);
    }

    //下架商品
    @DeleteMapping("/api/offGoods/{id}")
    @ResponseBody
    public AjaxResult offGoods(@PathVariable int id) {
        AjaxResult ajaxResult = new AjaxResult();
        goodsService.deleteGoodsByPrimaryKey(id);
        return new AjaxResult().setData(true);
    }
}
