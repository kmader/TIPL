function opts=frot(pts,rthe,zcent)
trot=[[cos(rthe),sin(rthe)];[-sin(rthe),cos(rthe)]];
Arot=eye(3);Arot(1:2,1:2)=trot;
centpts=(zcent(:)*ones(1,size(pts,1)))';
pts-centpts;
opts=(Arot*(pts-centpts)')'+centpts;