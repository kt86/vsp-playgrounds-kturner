package playground.kturner.freightKt.preWork;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.vehicles.Vehicle;

import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;

class CreateUCCCarriers {

	/*
	 * !!!ACHTUNG: MEthoden sind veraltet: siehe stattdessen; freigtKt.UccCarrierCreator
	 * @author: Kturner
	 * Ziele:
	 *  1.) Analyse der Carrier aus dem Schroeder/Liedtke-Berlin-Szenario.
	 *  2.) Erstellung eines Carrier-Files mit der Kette der größten Nachfage (Beachte, dass dabei Frozen, dry, Fresh) 
	 * 	jeweils eigene Carrier mit eigener Flotten zsammensetzung sind.) -> Hier aldi
	 *  3.) Links der Umweltzone einlesen (Maut-file) und die Nachfrage entsprechend auf UCC-Carrier mit ihren Depots und den Elektrofahrzeugen aufteilen.
	 */

	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/Case_KT/" ;
	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/";

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "network" ;
	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
	private static final String CARRIERS_2EXTRACT = "aldi" ;			//Retailer Name, der herausselektiert werden soll.
	private static final ArrayList<String> LEZLinkIdsString = new  ArrayList<String>(Arrays.asList("28930", "28929", "28928", "28927", "28912", "28911", "28826", "28825", "28824", "28816", "28814", "28812", "28810", "28808", "28807", "28804", "28803", "28712", "28711", "28710", "28709", "28706", "28705", "28704", "28703", "28702", "28701", "28700", "28699", "28698", "28697", "28690", "28689", "28688", "28687", "28686", "28685", "28684", "28683", "28682", "28681", "28678", "28677", "28675", "28674", "28673", "28672", "28671", "28670", "28669", "28668", "28667", "28666", "28665", "28664", "28663", "28662", "28661", "28660", "28659", "28658", "28657", "28656", "28655", "28654", "28653", "28652", "28651", "28650", "28649", "28648", "28647", "28646", "28645", "28644", "28643", "28642", "28641", "28640", "28639", "28638", "28637", "28636", "28635", "28634", "28633", "28632", "28631", "28630", "28629", "28628", "28627", "28626", "28625", "28624", "28623", "28622", "28621", "28620", "28619", "28618", "28617", "28616", "28615", "28614", "28613", "28612", "28611", "28610", "28609", "28608", "28607", "28606", "28605", "28604", "28603", "28602", "28601", "28600", "28599", "28598", "28597", "28596", "28595", "28594", "28593", "28550", "28549", "28548", "28547", "28546", "28545", "28544", "28543", "28542", "28541", "28540", "28539", "28538", "28537", "28536", "28535", "28534", "28533", "28532", "28531", "28530", "28529", "28527", "28526", "28525", "28524", "28522", "28521", "28520", "28519", "28518", "28517", "28516", "28515", "28514", "28513", "28512", "28511", "28510", "28509", "28508", "28507", "28506", "28505", "28504", "28503", "28502", "28501", "28500", "28498", "28496", "28494", "28492", "28491", "28490", "28489", "28488", "28487", "28486", "28485", "28484", "28483", "28482", "28481", "28480", "28479", "28478", "28477", "28476", "28475", "28474", "28473", "28472", "28471", "28470", "28469", "28467", "28466", "28465", "28464", "28463", "28462", "28461", "28460", "28459", "28458", "28457", "28456", "28455", "28453", "28452", "28451", "28450", "28449", "28448", "28446", "28444", "28443", "28442", "28441", "28440", "28439", "28438", "28437", "28436", "28435", "28433", "28432", "28431", "28430", "28429", "28428", "28427", "28426", "28425", "28424", "28423", "28422", "28421", "28420", "28418", "28417", "28415", "28414", "28413", "28412", "28411", "28410", "28409", "28408", "28407", "28406", "28405", "28404", "28403", "28402", "28401", "28400", "28399", "28398", "28397", "28396", "28395", "28394", "28393", "28392", "28391", "28390", "28389", "28388", "28387", "28386", "28385", "28384", "28383", "28382", "28381", "28380", "28379", "28376", "28375", "28374", "28373", "28372", "28371", "28370", "28368", "28367", "28366", "28365", "28364", "28363", "28362", "28361", "28360", "28359", "28358", "28357", "28356", "28355", "28354", "28353", "28352", "28351", "28350", "28349", "28348", "28347", "28346", "28345", "28344", "28343", "28342", "28341", "28340", "28339", "28338", "28337", "28336", "28335", "28334", "28333", "28332", "28331", "28330", "28329", "28328", "28327", "28326", "28325", "28324", "28323", "28322", "28321", "28320", "28319", "28318", "28317", "28316", "28315", "28314", "28313", "28312", "28311", "28310", "28309", "28308", "28307", "28306", "28305", "28304", "28303", "28302", "28301", "28300", "28299", "28298", "28297", "28296", "28295", "28294", "28293", "28292", "28291", "28290", "28289", "28288", "28287", "28286", "28285", "28284", "28283", "28282", "28281", "28280", "28279", "28276", "28275", "28274", "28273", "28272", "28271", "28270", "28269", "28268", "28267", "28266", "28265", "28264", "28263", "28262", "28261", "28260", "28259", "28258", "28257", "28256", "28255", "28254", "28253", "28252", "28251", "28250", "28249", "28248", "28247", "28246", "28245", "28244", "28243", "28242", "28241", "28240", "28239", "28238", "28237", "28236", "28235", "28234", "28233", "28232", "28231", "28230", "28229", "28228", "28227", "28226", "28225", "28224", "28223", "28222", "28221", "28220", "28219", "28218", "28217", "28216", "28215", "28214", "28213", "28212", "28211", "28210", "28209", "28208", "28207", "28206", "28205", "28204", "28203", "28202", "28201", "28200", "28198", "28197", "28196", "28195", "28194", "28193", "28192", "28191", "28190", "28189", "28188", "28187", "28186", "28185", "28184", "28183", "28182", "28181", "28180", "28179", "8357", "8179", "8023", "8022", "8019", "7857", "7854", "7682", "7672", "7671", "7666", "7665", "7644", "7642", "7641", "7640", "7639", "7638", "7637", "7636", "7635", "7634", "7633", "7632", "7631", "7630", "7629", "7628", "7627", "7626", "7625", "7624", "7623", "7622", "7621", "7620", "7619", "7618", "7617", "7616", "7615", "7554", "7553", "7552", "7551", "7550", "7549", "7548", "7547", "7546", "7545", "7544", "7543", "7542", "7541", "7540", "7539", "7538", "7537", "7536", "7535", "7534", "7533", "7532", "7531", "7530", "7529", "7528", "7527", "7502", "7501", "7500", "7499", "7398", "7397", "7396", "7394", "7393", "7392", "7391", "7380", "7379", "7378", "7377", "7376", "7375", "7374", "7373", "7372", "7371", "7370", "7369", "7368", "7367", "7366", "7365", "7364", "7363", "7362", "7361", "7354", "7353", "7352", "7351", "7348", "7347", "7346", "7345", "7344", "7343", "7342", "7341", "7340", "7339", "7338", "7335", "7333", "7332", "7331", "7329", "7328", "7325", "7323", "7302", "7301", "7300", "7299", "7292", "7291", "7278", "7276", "7275", "7268", "7267", "7256", "7255", "7248", "7247", "7246", "7245", "7202", "7201", "7200", "7199", "7171", "7170", "7168", "7167", "7166", "7165", "7164", "7163", "7162", "7161", "7160", "7159", "7158", "7157", "7124", "7123", "7122", "7121", "7108", "7107", "7106", "7105", "7104", "7103", "7102", "7101", "7100", "7099", "7076", "7075", "7032", "7031", "7026", "7025", "7023", "7021", "7020", "7018", "7017", "7006", "7005", "7004", "7003", "7002", "7001", "6980", "6979", "6978", "6977", "6956", "6955", "6954", "6953", "6952", "6951", "6950", "6949", "6946", "6888", "6856", "6846", "6770", "6700", "6699", "6698", "6697", "6696", "6695", "6694", "6693", "6692", "6691", "6690", "6689", "6688", "6687", "6686", "6685", "6664", "6663", "6662", "6661", "6660", "6659", "6658", "6657", "6656", "6655", "6654", "6653", "6652", "6651", "6636", "6633", "6632", "6631", "6628", "6627", "6626", "6625", "6620", "6619", "6618", "6617", "6616", "6615", "6588", "6587", "6492", "6491", "6460", "6459", "6458", "6457", "5396", "5395", "5390", "5389", "5388", "5387", "5386", "5384", "5383", "5382", "5381", "5380", "5379", "5378", "5377", "5376", "5375", "5374", "5373", "5372", "5371", "5370", "5369", "5368", "5367", "5364", "5363", "5351", "5346", "5345", "5344", "5343", "5342", "5341", "5340", "5339", "5338", "5337", "5336", "5335", "5334", "5333", "5332", "5331", "5330", "5329", "5328", "5327", "5326", "5325", "5324", "5323", "5322", "5321", "5320", "5319", "5318", "5317", "5316", "5315", "5306", "5305", "5304", "5303", "5302", "5301", "5300", "5298", "5297", "5296", "5295", "5294", "5293", "5292", "5291", "5290", "5289", "5288", "5287", "5286", "5285", "5284", "5283", "5282", "5281", "5280", "5279", "5278", "5277", "5276", "5275", "5274", "5273", "5272", "5271", "5270", "5269", "5268", "5267", "5252", "5251", "5131", "5130", "5129", "5128", "5127", "5126", "5125", "5124", "5123", "5122", "5114", "5113", "5112", "5111", "5109", "5098", "5096", "5095", "5094", "5090", "5089", "5083", "5074", "5073", "5072", "5071", "5070", "5069", "5068", "5067", "5066", "5065", "5060", "5059", "5058", "5057", "5056", "5055", "5054", "5053", "5052", "5051", "5050", "5049", "5044", "5043", "5042", "5041", "5040", "5039", "5038", "5037", "5032", "5031", "5030", "5029", "5028", "5027", "5025", "5019", "5000", "4999", "4998", "4997", "4770", "4769", "4746", "4745", "4744", "4743", "4742", "4741", "4530", "4529", "4521", "4260", "4259", "4258", "4257", "4256", "4255", "4254", "4253", "4252", "4251", "4250", "4249", "4248", "4247", "4246", "4245", "4244", "4243", "4242", "4241", "4240", "4239", "4238", "4237", "4236", "4235", "4234", "4233", "4232", "4231", "4230", "4229", "4228", "4227", "4226", "4225", "4224", "4223", "4222", "4221", "4220", "4219", "4218", "4217", "4216", "4215", "4214", "4213", "4212", "4211", "4210", "4209", "4208", "4207", "4206", "4205", "4204", "4203", "4202", "4201", "4200", "4199", "4198", "4197", "4196", "4195", "4193", "4192", "4191", "4190", "4189", "4188", "4187", "4186", "4185", "4184", "4183", "4182", "4181", "4180", "4179", "4178", "4177", "4176", "4175", "4174", "4173", "4172", "4171", "4170", "4169", "4168", "4167", "4166", "4165", "4164", "4163", "4160", "4159", "4156", "4155", "4154", "4153", "4148", "4147", "4146", "4145", "4144", "4143", "4142", "4141", "4140", "4139", "4138", "4137", "4136", "4135", "4134", "4133", "4132", "4131", "4130", "4129", "4128", "4127", "4126", "4125", "4124", "4123", "4122", "4121", "4120", "4119", "4118", "4117", "4116", "4114", "4113", "4112", "4111", "4110", "4108", "4107", "4106", "4105", "4104", "4103", "4102", "4100", "4099", "4098", "4097", "4096", "4095", "4094", "4093", "4092", "4091", "4090", "4089", "4087", "4086", "4085", "4084", "4082", "4081", "4080", "4079", "4078", "4077", "4076", "4075", "4074", "4073", "4072", "4071", "4070", "4069", "4068", "4067", "4066", "4064", "4063", "4062", "4061", "4060", "4059", "4058", "4057", "4056", "4055", "4054", "4053", "4052", "4051", "4050", "4049", "4048", "4047", "4046", "4045", "4044", "4043", "4042", "4041", "4040", "4039", "4037", "4036", "4035", "4034", "4033", "4032", "4031", "4030", "4029", "4028", "4027", "4026", "4025", "4024", "4023", "4022", "4021", "4020", "4019", "4018", "4017", "4016", "4015", "4014", "4013", "4011", "4010", "4009", "4008", "4007", "4005", "4002", "4001", "4000", "3999", "3998", "3997", "3992", "3991", "3990", "3989", "3988", "3987", "3986", "3985", "3984", "3983", "3982", "3981", "3980", "3979", "3978", "3977", "3976", "3975", "3974", "3973", "3972", "3971", "3968", "3967", "3966", "3965", "3964", "3963", "3962", "3961", "3960", "3959", "3958", "3956", "3955", "3954", "3953", "3952", "3951", "3950", "3949", "3948", "3947", "3946", "3945", "3944", "3943", "3942", "3941", "3940", "3939", "3938", "3937", "3936", "3935", "3934", "3933", "3932", "3928", "3927", "3926", "3925", "3924", "3923", "3922", "3921", "3920", "3919", "3918", "3917", "3916", "3915", "3914", "3913", "3912", "3911", "3910", "3909", "3908", "3907", "3906", "3904", "3903", "3902", "3901", "3900", "3899", "3898", "3897", "3896", "3895", "3894", "3893", "3892", "3891", "3890", "3889", "3888", "3887", "3886", "3885", "3884", "3883", "3882", "3881", "3880", "3879", "3878", "3877", "3876", "3875", "3874", "3873", "3872", "3871", "3870", "3869", "3868", "3867", "3866", "3865", "3864", "3863", "3862", "3861", "3858", "3857", "3856", "3855", "3854", "3853", "3852", "3851", "3850", "3849", "3848", "3847", "3846", "3845", "3844", "3843", "3842", "3841", "3840", "3839", "3838", "3837", "3836", "3835", "3834", "3833", "3832", "3831", "3830", "3829", "3828", "3827", "3826", "3825", "3824", "3823", "3822", "3821", "3820", "3819", "3818", "3817", "3816", "3815", "3814", "3813", "3810", "3809", "3806", "3802", "3800", "3796", "3795", "3792", "3791", "3790", "3789", "3786", "3785", "3782", "3781", "3780", "3779", "3778", "3777", "3776", "3775", "3774", "3773", "3772", "3771", "3770", "3769", "3768", "3767", "3766", "3765", "3764", "3763", "3762", "3761", "3760", "3759", "3758", "3757", "3754", "3753", "3752", "3751", "3750", "3749", "3748", "3747", "3746", "3745", "3744", "3743", "3742", "3741", "3740", "3739", "3738", "3737", "3736", "3735", "3734", "3733", "3732", "3730", "3726", "3725", "3724", "3723", "3722", "3721", "3720", "3719", "3718", "3715", "3714", "3713", "3712", "3711", "3703", "3701", "3700", "3699", "3698", "3697", "3696", "3695", "3694", "3693", "3690", "3689", "3686", "3685", "3684", "3683", "3682", "3681", "3680", "3679", "3678", "3677", "3676", "3675", "3674", "3673", "3672", "3671", "3670", "3669", "3668", "3667", "3666", "3665", "3664", "3663", "3662", "3661", "3660", "3659", "3657", "3655", "3652", "3651", "3647", "3642", "3641", "3624", "3623", "3622", "3621", "3614", "3613", "3610", "3609", "3608", "3607", "3604", "3602", "3601", "3600", "3599", "3598", "3597", "3596", "3595", "3594", "3593", "3592", "3591", "3590", "3589", "3588", "3587", "3586", "3585", "3584", "3583", "3582", "3581", "3580", "3579", "3578", "3577", "3576", "3575", "3574", "3573", "3572", "3570", "3567", "3566", "3565", "3564", "3563", "3561", "3559", "3558", "3557", "3556", "3555", "3548", "3547", "3546", "3545", "3544", "3542", "3541", "3540", "3539", "3538", "3537", "3536", "3535", "3534", "3533", "3532", "3531", "3529", "3528", "3527", "3526", "3525", "3524", "3523", "3522", "3521", "3520", "3519", "3518", "3517", "3516", "3515", "3514", "3513", "3512", "3511", "3510", "3509", "3508", "3507", "3506", "3505", "3504", "3503", "3502", "3501", "3500", "3499", "3498", "3497", "3496", "3495", "3494", "3493", "3492", "3491", "3490", "3489", "3488", "3487", "3486", "3485", "3484", "3483", "3482", "3481", "3480", "3478", "3476", "3475", "3474", "3473", "3472", "3471", "3470", "3469", "3468", "3467", "3466", "3465", "3462", "3461", "3458", "3457", "3456", "3455", "3454", "3453", "3452", "3451", "3450", "3449", "3448", "3447", "3444", "3443", "3442", "3441", "3440", "3439", "3438", "3437", "3435", "3420", "3419", "3417", "3416", "3415", "3414", "3412", "3410", "3409", "3408", "3407", "3405", "3404", "3403", "3402", "3401", "3400", "3399", "3398", "3397", "3396", "3395", "3394", "3393", "3392", "3391", "3390", "3389", "3388", "3387", "3386", "3385", "3384", "3383", "3382", "3381", "3380", "3379", "3378", "3377", "3376", "3375", "3374", "3373", "3372", "3371", "3370", "3369", "3368", "3367", "3366", "3365", "3364", "3363", "3362", "3361", "3360", "3359", "3345", "3344", "3343", "3342", "3341", "3340", "3339", "3338", "3337", "3336", "3335", "3334", "3333", "3332", "3331", "3330", "3329", "3328", "3327", "3326", "3325", "3324", "3323", "3322", "3321", "3320", "3319", "3318", "3317", "3316", "3315", "3313", "3312", "3311", "3310", "3309", "3308", "3307", "3306", "3305", "3304", "3303", "3301", "3300", "3299", "3298", "3297", "3296", "3295", "3294", "3293", "3292", "3291", "3290", "3289", "3288", "3287", "3286", "3284", "3283", "3282", "3281", "3279", "3278", "3277", "3276", "3275", "3274", "3272", "3271", "3270", "3269", "3268", "3267", "3266", "3265", "3264", "3263", "3262", "3261", "3260", "3259", "3258", "3257", "3256", "3255", "3254", "3253", "3252", "3251", "3250", "3249", "3248", "3247", "3246", "3245", "3244", "3243", "3234", "3233", "3232", "3230", "3229", "3228", "3226", "3225", "3224", "3223", "3222", "3221", "3220", "3219", "3218", "3217", "3216", "3215", "3214", "3212", "3211", "3209", "3208", "3206", "3205", "3204", "3203", "3201", "3200", "3199", "3198", "3197", "3196", "3195", "3194", "3193", "3192", "3191", "3190", "3189", "3188", "3187", "3186", "3185", "3184", "3183", "3182", "3181", "3180", "3179", "3178", "3177", "3176", "3175", "3174", "3173", "3172", "3171", "3170", "3169", "3168", "3167", "3166", "3165", "3164", "3163", "3162", "3161", "3160", "3159", "3158", "3157", "3156", "3155", "3154", "3153", "3152", "3151", "3150", "3149", "3138", "3137", "3125", "3124", "3123", "3122", "3119", "3117", "3116", "3115", "3114", "3112", "3111", "3110", "3109", "3108", "3107", "3106", "3105", "3104", "3103", "3102", "3101", "3100", "3099", "3098", "3097", "3096", "3095", "3094", "3093", "3092", "3091", "3090", "3089", "3088", "3087", "3086", "3085", "3084", "3083", "3082", "3081", "3080", "3079", "3078", "3077", "3069", "3056", "3055", "3054", "3053", "3052", "3051", "3050", "3049", "3048", "3047", "3046", "3045", "3044", "3043", "3042", "3041", "3040", "3039", "3038", "3037", "3035", "3034", "3033", "3031", "3030", "3029", "3028", "3027", "3026", "3025", "3023", "3022", "3021", "3020", "3019", "3018", "3017", "3016", "3015", "3014", "3013", "3012", "3011", "3010", "3009", "3008", "3007", "3006", "3005", "3004", "3003", "3002", "3001", "3000", "2999", "2998", "2997", "2996", "2995", "2990", "2989", "2988", "2986", "2985", "2984", "2983", "2982", "2981", "2975", "2972", "2971", "2970", "2969", "2968", "2967", "2966", "2965", "2964", "2963", "2961", "2960", "2959", "2958", "2957", "2956", "2955", "2946", "2945", "2944", "2942", "2941", "2940", "2938", "2937", "2936", "2935", "2934", "2933", "2932", "2931", "2930", "2929", "2928", "2927", "2926", "2925", "2924", "2923", "2922", "2921", "2920", "2919", "2918", "2917", "2916", "2915", "2914", "2913", "2912", "2911", "2910", "2909", "2908", "2907", "2906", "2905", "2903", "2902", "2898", "2897", "2896", "2895", "2894", "2893", "2892", "2891", "2890", "2889", "2888", "2887", "2876", "2875", "2872", "2871", "2869", "2868", "2226", "2225", "2010", "2009", "2008", "2007", "2006", "2005", "2004", "2003", "2002", "2001", "2000", "1999", "1998", "1997", "1996", "1995", "1994", "1993", "1992", "1991", "1990", "1989", "1988", "1987", "1986", "1985", "1984", "1983", "1982", "1981", "1980", "1979", "1974", "1973", "1970", "1969", "1968", "1967", "1966", "1965", "1964", "1963", "1962", "1961", "1960", "1959", "1958", "1957", "1956", "1955", "1953", "1952", "1951", "1950", "1949", "1948", "1947", "1946", "1945", "1936", "1935", "1934", "1933", "1932", "1931", "1930", "1929", "1926", "1924", "1923", "1922", "1921", "1920", "1919", "1918", "1917", "1916", "1915", "1914", "1913", "1912", "1911", "1910", "1909", "1908", "1907", "1906", "1905", "1904", "1903", "1902", "1901", "1900", "1899", "1898", "1897", "1896", "1895", "1894", "1893", "1892", "1891", "1890", "1889", "1888", "1887", "1886", "1885", "1884", "1883", "1882", "1880", "1879", "1878", "1877", "1876", "1874", "1873", "1871", "1866", "1864", "1863", "1862", "1861", "1860", "1859", "1858", "1856", "1855", "1854", "1851", "1849", "1848", "1847", "1846", "1845", "1844", "1842", "1841", "1840", "1838", "1837", "1836", "1835", "1834", "1833", "1832", "1831", "1830", "1829", "1828", "1827", "1826", "1825", "1824", "1823", "1822", "1821", "1820", "1819", "1818", "1817", "1816", "1815", "1814", "1813", "1812", "1811", "1810", "1809", "1808", "1807", "1806", "1805", "1804", "1803", "1802", "1801", "1800", "1799", "1798", "1797", "1796", "1795", "1794", "1793", "1792", "1791", "1790", "1789", "1788", "1787", "1786", "1785", "1784", "1783", "1782", "1781", "1766", "1765", "1764", "1762", "1761", "1760", "1759", "1758", "1757", "1756", "1755", "1754", "1753", "1752", "1750", "1749", "1748", "1747", "1746", "1745", "1744", "1743", "1742", "1741", "1740", "1739", "1738", "1737", "1735", "1734", "1733", "1732", "1731", "1730", "1729", "1728", "1727", "1726", "1725", "1724", "1723", "1722", "1721", "1720", "1719", "1718", "1717", "1716", "1715", "1714", "1713", "1712", "1711", "1710", "1709", "1708", "1707", "1706", "1705", "1704", "1703", "1702", "1701", "1700", "1699", "1698", "1697", "1696", "1695", "1694", "1693", "1692", "1691", "1690", "1689", "1688", "1687", "1686", "1685", "1654", "1653", "1652", "1651", "1648", "1647", "1646", "1645", "1644", "1643", "1642", "1641", "1639", "1638", "1637", "1636", "1635", "1634", "1633", "1632", "1631", "1630", "1629", "1589", "1585", "1499", "1498", "1497", "1496", "1495", "1494", "1493", "1491", "1490", "1489", "1488", "1487", "1486", "1485", "1484", "1483", "1482", "1481", "1480", "1478", "1477", "1476", "1475", "1474", "1473", "1472", "1471", "1468", "1467", "1466", "1465", "1460", "1459", "1458", "1457", "1448", "1445", "1424", "1423", "1422", "1421", "1420", "1419", "1418", "1417", "1416", "1415", "1414", "1413", "1412", "1411", "1410", "1409", "1408", "1407", "1406", "1405", "1404", "1403", "1402", "1401", "1400", "1399", "1398", "1397", "1396", "1395", "1394", "1393", "1392", "1391", "1390", "1389", "1388", "1387", "1386", "1385", "1384", "1383", "1382", "1381", "1380", "1379", "1378", "1377", "1376", "1375", "1374", "1373", "1372", "1369", "1368", "1367", "1366", "1365", "1364", "1363", "1362", "1361", "1360", "1359", "1358", "1357", "1356", "1355", "1354", "1353", "1347", "1333", "1332", "1296", "1295", "1294", "1293", "1292", "1291", "1290", "1289", "1288", "1287", "1286", "1285", "1284", "1283", "1282", "1281", "1280", "1278", "1277", "1276", "1275", "1274", "1273", "1272", "1271", "1270", "1269", "1268", "1267", "1266", "1265", "1264", "1263", "1262", "1261", "1260", "1259", "1258", "1257", "1256", "1255", "1254", "1253", "1252", "1251", "1250", "1249", "1248", "1247", "1246", "1245", "1244", "1243", "1242", "1241", "1240", "1239", "1238", "1237", "1236", "1235", "1233", "1232", "1231", "1230", "1228", "1227", "1208", "1207", "1206", "1205", "1204", "1203", "1202", "1201", "1200", "1199", "1198", "1197", "1196", "1195", "1194", "1193", "1192", "1191", "1190", "1189", "1188", "1187", "1186", "1185", "1184", "1183", "1182", "1181", "1180", "1179", "1178", "1177", "1176", "1175", "1174", "1173", "1172", "1171", "1170", "1169", "1168", "1167", "1164", "1163", "1162", "1161", "1160", "1159", "1158", "1157", "1156", "1155", "1124", "1120", "1114", "1113", "1112", "1111", "1110", "1109", "1105", "1104", "1103", "1095", "1062", "1061", "1060", "1059", "1050", "1049", "1047", "934", "931", "930", "929", "926", "925", "918", "917", "916", "915", "860", "859", "522", "521", "430", "429", "428", "427", "259", "246", "197", "186", "185", "142", "79", "78", "77", "74", "73", "72", "71", "64", "62", "61", "2", "1")); 

	//Ende  Namesdefinition Berlin


	//	//Beginn Namesdefinition KT Für Test-Szenario (Grid)
	//	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/" ;
	//	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Matsim/Demand/Grid/UCC2/" ;
	//	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/" ;	
	//
	//	//Dateinamen ohne XML-Endung
	//	private static final String NETFILE_NAME = "grid-network" ;
	//	private static final String VEHTYPES_NAME = "grid-vehTypes_kt" ;
	//	private static final String CARRIERS_NAME = "grid-carrier" ;
	//	private static final String CARRIERS_OUT_NAME = "grid-algorithm" ;
	//	private static final ArrayList<String> LEZLinkIdsString = new  ArrayList<String>(Arrays.asList("i(4,4)R" ,"i(5,4)R" ,"i(6,4)R" ,"i(7,4)R" ,"i(4,5)" ,"i(5,5)" ,"i(6,5)" ,"i(7,5)" ,"i(4,6)R" ,"i(5,6)R" ,"i(6,6)R" ,"i(7,6)R" ,"j(4,4)R" ,"j(4,5)R" ,"j(4,6)R" ,"j(4,7)R" ,"j(5,4)" ,"j(5,5)" ,"j(5,6)" ,"j(5,7)" ,"j(6,4)R" ,"j(6,5)R" ,"j(6,6)R" ,"j(6,7)R"));
	//	//Ende Namesdefinition Grid


	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPES_NAME + ".xml";
	private static final String CARRIERFILE = INPUT_DIR + CARRIERS_NAME + ".xml" ;
	private static final String CARRIEROUTFILE = OUTPUT_DIR + CARRIERS_2EXTRACT + ".xml";
	private static final String CARRIER_SPLIT_OUTFILE = OUTPUT_DIR + "carrier_" + CARRIERS_2EXTRACT + "_splitted.xml";
	
	static Carriers carriers = new Carriers() ;
	static CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		//Carrier-Stuff
//		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERFILE) ;
//		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

//		calcInfosPerCarrier(carriers);	//Step 1: Analyse der vorhandenen Carrier
		Carriers extractedCarriers = extractCarrier(carriers, CARRIERS_2EXTRACT); //Step 2a: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		
		new CarrierPlanXmlWriterV2(extractedCarriers).write(CARRIEROUTFILE) ;
		Carriers splittedCarriers = createUCCCarrier(extractedCarriers, LEZLinkIdsString);	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
		splittedCarriers = renameVehId(splittedCarriers); 							//Step4: VehId je Carrier einzigartig machen, da sonst weitere Vorkommen ingnoriert werden (und somit nicht alle Depots genutzt werden).
		new CarrierPlanXmlWriterV2(splittedCarriers).write(CARRIER_SPLIT_OUTFILE);
		
		System.out.println("### ENDE ###");
	}
	




	//Carrier Step 1: Analyse der vorhandenen Carrier
	private static void calcInfosPerCarrier(Carriers carriers) {

		File carrierInfoFile = new File(OUTPUT_DIR + "CarrierInformation.txt" );

		String carrierId;
		int nOfServices;
		int demand;
		int nOfDepots;
		ArrayList<String> depotLinks = new ArrayList<String>();
		ArrayList<String> vehTypes = new ArrayList<String>();

		for(Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();

			demand = 0; 
			nOfServices = 0;
			for (CarrierService service : carrier.getServices()){
				demand += service.getCapacityDemand();
				nOfServices++;
			}

			nOfDepots = 0;
			depotLinks.clear();
			vehTypes.clear();
			for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()){
				String depotLink = vehicle.getLocation().toString();
				if (!depotLinks.contains(depotLink)){
					depotLinks.add(depotLink);
					nOfDepots++;
				}
				if (!vehTypes.contains(vehicle.getVehicleType().getId().toString())){
					vehTypes.add(vehicle.getVehicleType().getId().toString());
				}
			}

			WriteCarrierInfos carrierInfoWriter = new WriteCarrierInfos(carrierInfoFile, carrierId, nOfServices, demand, nOfDepots, vehTypes) ;
		}	
	}

	//Carrier Step 2: Extrahieren einzelner Retailer (alle, die mit dem RetailerNamen beginnen)
	private static Carriers extractCarrier(Carriers carriers, String retailerName) {
		String carrierId;
		Carriers tempCarriers = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			carrierId = carrier.getId().toString();
			if (carrierId.startsWith(retailerName)){			//Carriername beginnt mit Retailername
				tempCarriers.addCarrier(carrier);
			}
		}
		return tempCarriers;
	}

	//Step3: Nachfrage auf Carrier UCC und normal aufteilen.
	private static Carriers createUCCCarrier(Carriers carriers,	ArrayList<String> lezlinkidsstring) {
		
		Carriers splittedCarriers = new Carriers(); // Carrierfile, welches beide Carrier enthält: sowohl UCC, als auch non UCC
		
		Set<Id<Link>> lezLinkIds = new HashSet<Id<Link>>();  //Link-Ids der Umweltzone (LEZ)
		for (String idString: LEZLinkIdsString) {
			lezLinkIds.add(Id.createLinkId(idString));
		}
		
		Set<CarrierService> serviceToRemove= new HashSet<CarrierService>(); 	//Liste der zum UCC-Carrier übertragenen Services -> wird später aus normalen Carrier entfernt
		
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier uccCarrier = CarrierImpl.newInstance(Id.create("UCC_"+carrier.getId() , Carrier.class));
			
			for (CarrierService service: carrier.getServices()) {
				if (lezLinkIds.contains(service.getLocationLinkId())){	//Service liegt in der Maut-Zone (=Umweltzone)
					uccCarrier.getServices().add(service);		//Füge Service zum UCC_Carrier hinzu
					serviceToRemove.add(service);
				}
			}
			
			for (CarrierService service: serviceToRemove){ //neue Schleife, da sonst innerhalb der Schleife das Set modifiziert wird..
				carrier.getServices().remove(service);	//und lösche ihn aus dem normalen Carrier raus
			}
			
			splittedCarriers.addCarrier(carrier); //bisherigen Carrier reinschreiben, darf auch ohne Service sein, da ggf während Laufzeit nachfrage erhält (Depot -> UCC).
			
			if (!uccCarrier.getServices().isEmpty()){		//keinen UCC ohne Nachfrage übernehmen.
				addVehicles(uccCarrier);
				uccCarrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
				splittedCarriers.addCarrier(uccCarrier);
			}
		}
		
		return splittedCarriers;
	}
	
	//Step3b: Fahrzeuge der UCC zuordnen
	//TODO: Links als Array mit übergeben -> flexibler gestalten.
	private static void addVehicles(Carrier uccCarrier) {
		
		Set<Id<Link>> uccDepotsLinkIds = new HashSet<Id<Link>>();
		//Für Berlin-Szenario:
		uccDepotsLinkIds.add(Id.createLinkId("6874"));	//Nord: Industriegebiet Berliner Großmarkt
		uccDepotsLinkIds.add(Id.createLinkId("3058"));  //Süd: Gewerbegebiet  Bessermerstraße
		uccDepotsLinkIds.add(Id.createLinkId("5468"));	//Ost: Gewerbegebiet Herzbergstr /Siegfriedstr.
		
		double uccOpeningTime = 8*3600.0;	// 08:00:00 Uhr
		double uccClosingTime = 21*3600.0;	// 21:00:00 Uhr
		
		if (uccCarrier.getId().toString().endsWith("TIEFKUEHL")){
			
			for (Id<Link> linkId : uccDepotsLinkIds ){
			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add( CarrierVehicle.Builder.newInstance(Id.create("light8telectro_frozen", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro_frozen", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());
			}
//			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(cv_8f);
		} else {
			
			for (Id<Link> linkId : uccDepotsLinkIds ){
				uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());
			
			uccCarrier.getCarrierCapabilities().getCarrierVehicles().add(CarrierVehicle.Builder.newInstance(Id.create("medium18telectro", Vehicle.class), linkId)
					.setType(vehicleTypes.getVehicleTypes().get(Id.create("medium18telectro", VehicleType.class)))
					.setEarliestStart(uccOpeningTime).setLatestEnd(uccClosingTime)
					.build());

			}
			
		}
		
	}

	//Step4: VehicleId je Carrier um Location erweitern, da sonst weitere Vorkommen auf Grund gleicher VehicleId ingnoriert werden 
	//		und somit nicht alle Depots genutzt werden.
	private static Carriers renameVehId(Carriers carriers) {
		
		for (Carrier carrier : carriers.getCarriers().values()){
			//da Änderung der vorhanden Fahrzeuge sonst nicht ging, Umweg über newInstance und setzen der Eigenschaften.
			CarrierCapabilities tempCc = CarrierCapabilities.newInstance();
			tempCc.setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
			//Vehicle neu erstellen, da setVehicleId nicht verfügbar.
			for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				//
				tempCc.getCarrierVehicles().add(CarrierVehicle.Builder
						.newInstance(Id.create(cv.getVehicleId().toString() +"_" + cv.getLocation().toString(), Vehicle.class), cv.getLocation())
						.setType(cv.getVehicleType())
						.setEarliestStart(cv.getEarliestStartTime()).setLatestEnd(cv.getLatestEndTime())
						.build());
			}
			carrier.setCarrierCapabilities(tempCc);
			
		}		
		return carriers;
	}

	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

}
