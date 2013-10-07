classdef TIPL < handle
    % a class for the TIPL tools wrapping many of the common functions into
    % easily used matlab
    properties(GetAccess = 'public', SetAccess = 'private')
        %important properties
        plane_fn; line_fn;
    end
    
    methods
        function obj=TIPL
        % class constructor
            javaaddpath('/afs/psi.ch/project/tipl/jar/TIPL.jar');
            obj.plane_fn=javaObjectEDT('tipl.tests.TestFImages$DiagonalPlaneFunction');
            obj.line_fn=javaObjectEDT('tipl.tests.TestFImages$LinesFunction');
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
        function wrapped_im=wrap_fn(im_fun,side_len)
            wrapped_im=javaMethod('wrapIt','tipl.tests.TestFImages',side_len,im_fun);
        end
        function slice=get_slice(im_data,slice_num,im_type) 
            dims=TIPL.d3_to_array(im_data.getDim());
            slice=reshape(im_data.getPolyImage(slice_num,im_type),dims(1:2));
        end
        function out_image=resize(in_image,start_pos,dim_rng)
            % static function to perform resize using the resize plugin
            im_pos=TIPL.d3_to_array(in_image.getPos());
            resize_plugin=tipl.tools.Resize();
            in_images=javaArray('tipl.formats.TImgRO',1);
            in_images(1)=in_image;
            resize_plugin.LoadImages(in_images);
            resize_plugin.cutROI(TIPL.d3int(start_pos+im_pos),TIPL.d3int(dim_rng))
            resize_plugin.execute()
            out_images=resize_plugin.ExportImages(in_image);
            out_image=out_images(1);
        end
    end
end