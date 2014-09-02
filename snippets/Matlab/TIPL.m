classdef TIPL < handle
    % a class for the TIPL tools wrapping many of the common functions into
    % easily used matlab
    properties(GetAccess = 'public', SetAccess = 'private')
        %important properties
        plane_fn; line_fn; x_fun; z_fun; y_fun
    end
    
    methods
        function obj=TIPL
        % class constructor
            javaaddpath('/afs/psi.ch/project/tipl/jar/TIPL.jar');
            obj.plane_fn=javaObjectEDT('tipl.tests.TestFImages$DiagonalPlaneFunction');
            obj.line_fn=javaObjectEDT('tipl.tests.TestFImages$LinesFunction');
            obj.x_fun=javaObjectEDT('tipl.tests.TestFImages$ProgXImage');
            obj.y_fun=javaObjectEDT('tipl.tests.TestFImages$ProgYImage');
            obj.z_fun=javaObjectEDT('tipl.tests.TestFImages$ProgZImage');
        end
        function wrapped_im=wrap_img(obj,im_fun,side_len,im_type)
            % wrap a function in an image
            % test function 
            %   h=TIPL();p=h.wrap_img(h.x_fun,10,1)
            % p.im_data.setPos(h.d3int([100,200,300]))
            wrapped_im=TIPLImage(obj,TIPL.wrap_fun_as(im_fun,side_len,im_type));
        end
    end
    methods(Static=true)
        function dout=d3int(x) 
            dout=tipl.util.D3int(x(1),x(2),x(3));
        end
        function dout=d3float(x)
            dout=tipl.util.D3float(x(1),x(2),x(3));
        end
        function dout=d3_to_array(x)
            dout=[x.x,x.y,x.z];
        end
        function c_img=read_image(file_name)
            c_img=tipl.util.TImgTools.ReadTImg(file_name);
        end
        function wrapped_im=wrap_fun(im_fun,side_len)
            wrapped_im=javaMethod('wrapIt','tipl.tests.TestFImages',side_len,im_fun);
        end
        function wrapped_im=wrap_fun_as(im_fun,side_len,im_type)
            wrapped_im=javaMethod('wrapItAs','tipl.tests.TestFImages',side_len,im_fun,im_type);
        end
        
        function slice=get_slice(im_data,slice_num,im_type) 
            dims=TIPL.d3_to_array(im_data.getDim());
            slice=reshape(im_data.getPolyImage(slice_num,im_type),dims(1:2))';
        end
        function out_image=resize(in_image,start_pos,dim_rng)
            % static function to perform resize using the resize plugin
            % resizeImg=t.resize(testImg.im_data,t.d3_to_array(testImg.im_d
            % ata.getPos()),[testImg.dim(1:2) 1])
            resize_plugin=tipl.tools.Resize();
            in_images=javaArray('tipl.formats.TImgRO',1);
            in_images(1)=in_image;
            resize_plugin.LoadImages(in_images);
            resize_plugin.cutROI(TIPL.d3int(start_pos),TIPL.d3int(dim_rng))
            resize_plugin.execute();
            out_images=resize_plugin.ExportImages(in_image);
            out_image=out_images(1);
        end
        
        
    end
end