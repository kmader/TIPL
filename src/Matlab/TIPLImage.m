classdef TIPLImage < handle
    % a class wrapping TImg objects in matlab with several of the more
    % useful functions already built in 
    % Example of standard usage 
    % h=TIPL();
    % t=TIPLImage(h,'/afs/psi.ch/project/tipl/test/foamSample/labels.tif')
    % imagesc(t.get_slice(0,3))
    %  h=TIPL(); t=TIPLImage(h,'/afs/psi.ch/project/tipl/test/foamSample/labels.tif'); imagesc(t.get_slice(0,3))
    properties(GetAccess = 'public', SetAccess = 'private')
        %important properties
        im_data; in_tipl; dim
    end
    
    methods
        function obj=TIPLImage(inTIPL,file_name)
        % class constructor
            obj.in_tipl=inTIPL;
            if isstr(file_name)
                obj.im_data=obj.in_tipl.read_image(file_name);
            else
                obj.im_data=file_name; % it is not a name rather an image object
            end
            obj.dim=obj.in_tipl.d3_to_array(obj.im_data.getDim());
        end
        function slice=get_slice(obj,slice_num,im_type) 
            slice=obj.in_tipl.get_slice(obj.im_data,slice_num,im_type);
        end
        function preview(obj,slice_num)
            imagesc(obj.get_slice(slice_num,3))
        end
        function out_data=get_all(obj,im_type)
            out_data=zeros(obj.dim)
            for i=0:obj.dim(3)
                out_data(:,:,i)=obj.get_slice(i,im_type);
            end
        end
        
        function new_image=resize(obj)
            % resize by clicking bounding boxes
            disp('Select Bottom Corner');
            pos1=obj.get_point(0);
            disp('Select Top Corner');
            pos2=obj.get_point(obj.dim(3)-1);
            box_vec=round([pos1;pos2]);
            start_pos=[min(box_vec)]
            end_pos=[max(box_vec)]
            dim_rng=end_pos-start_pos
            out_data=obj.in_tipl.resize(obj.im_data,start_pos,dim_rng)
            new_image=TIPLImage(obj.in_tipl,out_data);
        end
        function cur_point=get_point(obj,start_z)
            % get a point in the image
            c=0
            cur_z=start_z;
            while c~=2
                cur_z=median([0,obj.dim(3)-1,cur_z]);
                imagesc(obj.get_slice(cur_z,3));
                title(['Slice: ' num2str(cur_z) '/' num2str(obj.dim(3)) ' Left click to go back a slice, middle to accept, right to go forward']);
                [x,y,c]=ginput(1);
                if c==1
                    cur_z=cur_z-1;
                end
                if c==3
                    cur_z=cur_z+1;
                end
            end
            cur_point=round([x,y,cur_z]);
        end
    end
end